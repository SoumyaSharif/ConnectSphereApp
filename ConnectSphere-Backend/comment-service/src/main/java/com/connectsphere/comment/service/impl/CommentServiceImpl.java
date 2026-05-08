package com.connectsphere.comment.service.impl;

import com.connectsphere.comment.dto.CommentDto;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl {

    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public CommentDto addComment(String postId, String authorId, String content, String parentCommentId) {
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content(content)
                .parentCommentId(parentCommentId)
                .build();
        comment = commentRepository.save(comment);
        // Notify post service to increment comment count
        try {
            restTemplate.patchForObject(
                "http://post-service/api/v1/posts/{postId}/comments/increment", null, Void.class, postId);
        } catch (Exception e) {
            log.warn("Failed to increment post comment count: {}", e.getMessage());
        }
        sendCommentNotification(comment);
        return mapToDto(comment);
    }

    public Page<CommentDto> getCommentsByPost(String postId, int page, int size) {
        return commentRepository
            .findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(postId, PageRequest.of(page, size))
            .map(this::mapToDto);
    }

    public List<CommentDto> getReplies(String commentId) {
        return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId)
            .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public CommentDto getCommentById(String commentId) {
        return commentRepository.findById(commentId)
            .filter(c -> !c.isDeleted())
            .map(this::mapToDto)
            .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
    }

    @Transactional
    public CommentDto updateComment(String commentId, String requesterId, String content) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getAuthorId().equals(requesterId)) throw new RuntimeException("Forbidden");
        comment.setContent(content);
        return mapToDto(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(String commentId, String requesterId, String requesterRole) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        boolean isAdmin = "ADMIN".equals(requesterRole);
        if (!isAdmin && !comment.getAuthorId().equals(requesterId)) throw new RuntimeException("Forbidden");
        comment.setDeleted(true);
        commentRepository.save(comment);
        try {
            restTemplate.patchForObject(
                "http://post-service/api/v1/posts/{postId}/comments/decrement", null, Void.class, comment.getPostId());
        } catch (Exception e) {
            log.warn("Failed to decrement post comment count: {}", e.getMessage());
        }
    }

    @Transactional
    public void likeComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
    }

    @Transactional
    public void unlikeComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        commentRepository.save(comment);
    }

    public long getCommentCount(String postId) {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }

    public long getTotalCommentCount() {
        return commentRepository.countByIsDeletedFalse();
    }

    private CommentDto mapToDto(Comment c) {
        return CommentDto.builder()
            .commentId(c.getCommentId())
            .postId(c.getPostId())
            .authorId(c.getAuthorId())
            .parentCommentId(c.getParentCommentId())
            .content(c.getContent())
            .likesCount(c.getLikesCount())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }

    private void sendCommentNotification(Comment comment) {
        try {
            if (comment.getParentCommentId() == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> post = restTemplate.getForObject(
                        "http://post-service/api/v1/posts/{id}",
                        Map.class,
                        comment.getPostId()
                );

                String recipientId = post != null ? (String) post.get("authorId") : null;
                if (recipientId == null || recipientId.equals(comment.getAuthorId())) {
                    return;
                }

                createNotification(
                        recipientId,
                        comment.getAuthorId(),
                        "COMMENT",
                        "commented on your post.",
                        comment.getPostId(),
                        "POST",
                        "/post/" + comment.getPostId()
                );
                return;
            }

            Comment parent = commentRepository.findById(comment.getParentCommentId()).orElse(null);
            if (parent == null || parent.getAuthorId().equals(comment.getAuthorId())) {
                return;
            }

            createNotification(
                    parent.getAuthorId(),
                    comment.getAuthorId(),
                    "REPLY",
                    "replied to your comment.",
                    comment.getPostId(),
                    "COMMENT",
                    "/post/" + comment.getPostId()
            );
        } catch (Exception e) {
            log.warn("Failed to create comment notification: {}", e.getMessage());
        }
    }

    private void createNotification(String recipientId,
                                    String actorId,
                                    String type,
                                    String message,
                                    String targetId,
                                    String targetType,
                                    String deepLinkUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", type);
        payload.put("message", message);
        payload.put("targetId", targetId);
        payload.put("targetType", targetType);
        payload.put("deepLinkUrl", deepLinkUrl);
        restTemplate.postForObject("http://notification-service/api/v1/notifications/internal", payload, Void.class);
    }
}
