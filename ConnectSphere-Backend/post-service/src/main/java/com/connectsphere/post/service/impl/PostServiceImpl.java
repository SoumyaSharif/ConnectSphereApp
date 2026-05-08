package com.connectsphere.post.service.impl;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.PostDto;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.exception.ForbiddenException;
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.kafka.PostEventProducer;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.PostService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PostDto createPost(String authorId, CreatePostRequest request) {
        validatePostPayload(request);

        Post post = Post.builder()
                .authorId(authorId)
                .content(normalizeContent(request.getContent()))
                .mediaUrls(serializeUrls(request.getMediaUrls()))
                .postType(request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()
                        ? Post.PostType.MEDIA : Post.PostType.TEXT)
                .visibility(request.getVisibility())
                .build();

        post = postRepository.save(post);
        eventProducer.sendPostCreatedEvent(post);
        log.info("Post created: {}", post.getPostId());
        return mapToDto(post);
    }

    @Override
    public Page<PostDto> getHomeFeed(String requesterId, int page, int size) {
        return postRepository.findHomeFeed(requesterId, PageRequest.of(page, size)).map(this::mapToDto);
    }

    @Override
    public PostDto getPostById(String postId) {
        return mapToDto(findActivePost(postId));
    }

    @Override
    public Page<PostDto> getPostsByUser(String authorId, int page, int size) {
        return postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(
                authorId, PageRequest.of(page, size)).map(this::mapToDto);
    }

    @Override
    public Page<PostDto> getPublicFeed(int page, int size) {
        return postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                Post.Visibility.PUBLIC, PageRequest.of(page, size)).map(this::mapToDto);
    }

    @Override
    public Page<PostDto> getFeedForUser(List<String> followeeIds, int page, int size) {
        if (followeeIds == null || followeeIds.isEmpty()) {
            return getPublicFeed(page, size);
        }
        return postRepository.findFeedByAuthorIds(followeeIds, PageRequest.of(page, size)).map(this::mapToDto);
    }

    @Override
    @Transactional
    public PostDto updatePost(String postId, String requesterId, CreatePostRequest request) {
        validatePostPayload(request);

        Post post = findActivePost(postId);
        ensureOwnership(post, requesterId, null);

        post.setContent(normalizeContent(request.getContent()));
        post.setMediaUrls(serializeUrls(request.getMediaUrls()));
        post.setVisibility(request.getVisibility());
        return mapToDto(postRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(String postId, String requesterId, String requesterRole) {
        Post post = findActivePost(postId);
        ensureOwnership(post, requesterId, requesterRole);
        post.setDeleted(true);
        postRepository.save(post);
        eventProducer.sendPostDeletedEvent(postId);
    }

    @Override
    public Page<PostDto> searchPosts(String query, int page, int size) {
        return postRepository.searchByContent(query, PageRequest.of(page, size)).map(this::mapToDto);
    }

    @Override
    @Transactional
    public void incrementLikes(String postId) {
        Post post = findActivePost(postId);
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void decrementLikes(String postId) {
        Post post = findActivePost(postId);
        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void incrementComments(String postId) {
        Post post = findActivePost(postId);
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void decrementComments(String postId) {
        Post post = findActivePost(postId);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void incrementShares(String postId) {
        Post post = findActivePost(postId);
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void changeVisibility(String postId, String requesterId, Post.Visibility visibility) {
        Post post = findActivePost(postId);
        ensureOwnership(post, requesterId, null);
        post.setVisibility(visibility);
        postRepository.save(post);
    }

    @Override
    public long getPostCount(String authorId) {
        return postRepository.countByAuthorIdAndIsDeletedFalse(authorId);
    }

    @Override
    public long getTotalPostCount() {
        return postRepository.countByIsDeletedFalse();
    }

    // ---- Helpers ----

    private Post findActivePost(String postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
    }

    private void ensureOwnership(Post post, String requesterId, String requesterRole) {
        boolean isAdmin = "ADMIN".equals(requesterRole);
        if (!isAdmin && !post.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("You are not allowed to modify this post");
        }
    }

    private void validatePostPayload(CreatePostRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasMedia = request.getMediaUrls() != null && request.getMediaUrls().stream()
                .anyMatch(url -> url != null && !url.trim().isEmpty());

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Post content or media is required");
        }
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }

        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String serializeUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> deserializeUrls(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private PostDto mapToDto(Post post) {
        return PostDto.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .mediaUrls(deserializeUrls(post.getMediaUrls()))
                .postType(post.getPostType())
                .visibility(post.getVisibility())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
