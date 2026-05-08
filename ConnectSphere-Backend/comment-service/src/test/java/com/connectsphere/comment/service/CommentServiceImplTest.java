package com.connectsphere.comment.service;

import com.connectsphere.comment.dto.CommentDto;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Tests")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private CommentServiceImpl commentService;

    private Comment buildComment(String id, String postId, String authorId, boolean deleted) {
        Comment c = new Comment();
        c.setCommentId(id);
        c.setPostId(postId);
        c.setAuthorId(authorId);
        c.setContent("Test content");
        c.setDeleted(deleted);
        c.setLikesCount(0);
        return c;
    }

    @Test
    @DisplayName("addComment: saves comment and returns DTO, handles notification logic")
    void addComment_success() {
        Comment saved = buildComment("c1", "p1", "u1", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        // Mock post fetch for top-level comment notification
        when(restTemplate.getForObject("http://post-service/api/v1/posts/{id}", java.util.Map.class, "p1"))
                .thenReturn(java.util.Map.of("authorId", "postAuthorId"));

        CommentDto dto = commentService.addComment("p1", "u1", "Test content", null);

        assertThat(dto.getCommentId()).isEqualTo("c1");
        assertThat(dto.getAuthorId()).isEqualTo("u1");
        
        // Verify increment call
        verify(restTemplate).patchForObject(eq("http://post-service/api/v1/posts/{postId}/comments/increment"), isNull(), eq(Void.class), eq("p1"));
        
        // Verify notification call
        verify(restTemplate).postForObject(eq("http://notification-service/api/v1/notifications/internal"), any(java.util.Map.class), eq(Void.class));
    }

    @Test
    @DisplayName("addComment: handles exception in post increment")
    void addComment_incrementException() {
        Comment saved = buildComment("c1", "p1", "u1", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        when(restTemplate.patchForObject(anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Service down"));

        // Should not throw
        commentService.addComment("p1", "u1", "Test content", null);
    }

    @Test
    @DisplayName("addComment: reply triggers notification to parent author")
    void addComment_replyNotification() {
        Comment saved = buildComment("c2", "p1", "u1", false);
        saved.setParentCommentId("c1");
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        Comment parent = buildComment("c1", "p1", "parentAuthor", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(parent));

        commentService.addComment("p1", "u1", "Reply", "c1");

        // Verify notification call
        verify(restTemplate).postForObject(eq("http://notification-service/api/v1/notifications/internal"), any(java.util.Map.class), eq(Void.class));
    }

    @Test
    @DisplayName("addComment: ignores notification if commenting on own post or replying to self")
    void addComment_ownPostNoNotification() {
        Comment saved = buildComment("c1", "p1", "u1", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        // Post author is the same as commenter
        when(restTemplate.getForObject("http://post-service/api/v1/posts/{id}", java.util.Map.class, "p1"))
                .thenReturn(java.util.Map.of("authorId", "u1"));

        commentService.addComment("p1", "u1", "Test content", null);

        // No notification should be sent
        verify(restTemplate, never()).postForObject(eq("http://notification-service/api/v1/notifications/internal"), any(), any());
    }

    @Test
    @DisplayName("addComment: handles exception during notification creation")
    void addComment_notificationException() {
        Comment saved = buildComment("c1", "p1", "u1", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        when(restTemplate.getForObject(anyString(), eq(java.util.Map.class), anyString()))
                .thenThrow(new RuntimeException("Post service down"));

        // Should catch exception and not throw
        commentService.addComment("p1", "u1", "Test content", null);
    }

    // ─── getCommentById() ────────────────────────────────────────
    @Test
    @DisplayName("getCommentById: returns DTO for existing non-deleted comment")
    void getCommentById_found() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));

        CommentDto dto = commentService.getCommentById("c1");

        assertThat(dto.getCommentId()).isEqualTo("c1");
    }

    @Test
    @DisplayName("getCommentById: throws for deleted comment")
    void getCommentById_deletedThrows() {
        Comment deleted = buildComment("c1", "p1", "u1", true);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> commentService.getCommentById("c1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");
    }

    @Test
    @DisplayName("getCommentById: throws when comment does not exist")
    void getCommentById_notFound() {
        when(commentRepository.findById("c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById("c1"))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── updateComment() ─────────────────────────────────────────
    @Test
    @DisplayName("updateComment: updates content for owner")
    void updateComment_success() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        CommentDto dto = commentService.updateComment("c1", "u1", "Updated");

        assertThat(dto.getContent()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("updateComment: throws when requester is not author")
    void updateComment_notOwner_throws() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.updateComment("c1", "u-other", "Updated"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forbidden");
    }

    // ─── deleteComment() ─────────────────────────────────────────
    @Test
    @DisplayName("deleteComment: soft-deletes comment for owner")
    void deleteComment_owner_success() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        commentService.deleteComment("c1", "u1", "USER");

        assertThat(c.isDeleted()).isTrue();
        verify(commentRepository).save(c);
    }

    @Test
    @DisplayName("deleteComment: admin can delete any comment")
    void deleteComment_admin_success() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        commentService.deleteComment("c1", "admin-user", "ADMIN");

        assertThat(c.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteComment: handles exception in post decrement")
    void deleteComment_decrementException() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        when(restTemplate.patchForObject(anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Service down"));

        // Should not throw
        commentService.deleteComment("c1", "u1", "USER");
    }

    @Test
    @DisplayName("deleteComment: throws when non-owner non-admin tries to delete")
    void deleteComment_forbidden_throws() {
        Comment c = buildComment("c1", "p1", "u1", false);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteComment("c1", "u-other", "USER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forbidden");
    }

    // ─── likeComment() / unlikeComment() ─────────────────────────
    @Test
    @DisplayName("likeComment: increments like count")
    void likeComment_incrementsCount() {
        Comment c = buildComment("c1", "p1", "u1", false);
        c.setLikesCount(2);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        commentService.likeComment("c1");

        assertThat(c.getLikesCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("unlikeComment: decrements like count but not below 0")
    void unlikeComment_decrementsCount() {
        Comment c = buildComment("c1", "p1", "u1", false);
        c.setLikesCount(0);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);

        commentService.unlikeComment("c1");

        assertThat(c.getLikesCount()).isEqualTo(0);
    }

    // ─── counts ──────────────────────────────────────────────────
    @Test
    @DisplayName("getCommentCount: returns non-deleted count for a post")
    void getCommentCount() {
        when(commentRepository.countByPostIdAndIsDeletedFalse("p1")).thenReturn(5L);
        assertThat(commentService.getCommentCount("p1")).isEqualTo(5L);
    }

    @Test
    @DisplayName("getTotalCommentCount: returns all non-deleted comments")
    void getTotalCommentCount() {
        when(commentRepository.countByIsDeletedFalse()).thenReturn(99L);
        assertThat(commentService.getTotalCommentCount()).isEqualTo(99L);
    }

    // ─── getCommentsByPost() ─────────────────────────────────────
    @Test
    @DisplayName("getCommentsByPost: returns page of comment DTOs")
    void getCommentsByPost() {
        Comment c = buildComment("c1", "p1", "u1", false);
        Page<Comment> page = new PageImpl<>(List.of(c));
        when(commentRepository
                .findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
                        eq("p1"), any(PageRequest.class)))
                .thenReturn(page);

        Page<CommentDto> result = commentService.getCommentsByPost("p1", 0, 10);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPostId()).isEqualTo("p1");
    }
}
