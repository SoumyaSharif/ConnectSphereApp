package com.connectsphere.post.service;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.PostDto;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.exception.ForbiddenException;
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.kafka.PostEventProducer;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.impl.PostServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl Tests")
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private PostEventProducer eventProducer;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks private PostServiceImpl postService;

    private static final String AUTHOR_ID = "u1";
    private static final String POST_ID   = "p1";

    private Post activePost() {
        return Post.builder()
                .postId(POST_ID)
                .authorId(AUTHOR_ID)
                .content("Hello world")
                .postType(Post.PostType.TEXT)
                .visibility(Post.Visibility.PUBLIC)
                .isDeleted(false)
                .build();
    }

    private CreatePostRequest textRequest(String content) {
        CreatePostRequest r = new CreatePostRequest();
        r.setContent(content);
        r.setVisibility(Post.Visibility.PUBLIC);
        return r;
    }

    // ─── createPost() ────────────────────────────────────────────
    @Test
    @DisplayName("createPost: saves post and fires event")
    void createPost_success() {
        Post saved = activePost();
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostDto dto = postService.createPost(AUTHOR_ID, textRequest("Hello world"));

        assertThat(dto.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(dto.getContent()).isEqualTo("Hello world");
        verify(postRepository).save(any(Post.class));
        verify(eventProducer).sendPostCreatedEvent(any(Post.class));
    }

    @Test
    @DisplayName("createPost: throws when content and mediaUrls are both empty")
    void createPost_emptyPayload_throws() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("   ");
        req.setVisibility(Post.Visibility.PUBLIC);

        assertThatThrownBy(() -> postService.createPost(AUTHOR_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content or media is required");
    }

    // ─── getPostById() ───────────────────────────────────────────
    @Test
    @DisplayName("getPostById: returns DTO for active post")
    void getPostById_found() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(activePost()));
        PostDto dto = postService.getPostById(POST_ID);
        assertThat(dto.getPostId()).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("getPostById: throws for deleted post")
    void getPostById_deletedThrows() {
        Post deleted = activePost();
        deleted.setDeleted(true);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> postService.getPostById(POST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPostById: throws when post does not exist")
    void getPostById_notFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(POST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    // ─── updatePost() ────────────────────────────────────────────
    @Test
    @DisplayName("updatePost: owner can update their post")
    void updatePost_owner_success() {
        Post post = activePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        PostDto dto = postService.updatePost(POST_ID, AUTHOR_ID, textRequest("Updated!"));

        assertThat(dto.getContent()).isEqualTo("Updated!");
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("updatePost: non-owner throws ForbiddenException")
    void updatePost_notOwner_throws() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(activePost()));

        assertThatThrownBy(() -> postService.updatePost(POST_ID, "other-user", textRequest("Hack")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── deletePost() ────────────────────────────────────────────
    @Test
    @DisplayName("deletePost: owner soft-deletes post and fires event")
    void deletePost_owner_success() {
        Post post = activePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.deletePost(POST_ID, AUTHOR_ID, "USER");

        assertThat(post.isDeleted()).isTrue();
        verify(eventProducer).sendPostDeletedEvent(POST_ID);
    }

    @Test
    @DisplayName("deletePost: admin can delete any post")
    void deletePost_admin_success() {
        Post post = activePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.deletePost(POST_ID, "admin-id", "ADMIN");

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deletePost: non-owner user throws ForbiddenException")
    void deletePost_notOwner_throws() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(activePost()));

        assertThatThrownBy(() -> postService.deletePost(POST_ID, "hacker", "USER"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── incrementLikes() / decrementLikes() ─────────────────────
    @Test
    @DisplayName("incrementLikes: increases likesCount by 1")
    void incrementLikes() {
        Post post = activePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.incrementLikes(POST_ID);

        assertThat(post.getLikesCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("decrementLikes: decreases likesCount but not below 0")
    void decrementLikes_floor() {
        Post post = activePost(); // likesCount = 0
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.decrementLikes(POST_ID);

        assertThat(post.getLikesCount()).isEqualTo(0L);
    }

    // ─── incrementComments() / decrementComments() ───────────────
    @Test
    @DisplayName("incrementComments: increases commentsCount by 1")
    void incrementComments() {
        Post post = activePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.incrementComments(POST_ID);

        assertThat(post.getCommentsCount()).isEqualTo(1L);
    }

    // ─── getPostCount() ──────────────────────────────────────────
    @Test
    @DisplayName("getPostCount: returns count for author")
    void getPostCount() {
        when(postRepository.countByAuthorIdAndIsDeletedFalse(AUTHOR_ID)).thenReturn(5L);
        assertThat(postService.getPostCount(AUTHOR_ID)).isEqualTo(5L);
    }

    @Test
    @DisplayName("getTotalPostCount: returns all non-deleted posts")
    void getTotalPostCount() {
        when(postRepository.countByIsDeletedFalse()).thenReturn(250L);
        assertThat(postService.getTotalPostCount()).isEqualTo(250L);
    }

    // ─── getFeedForUser() ────────────────────────────────────────
    @Test
    @DisplayName("getFeedForUser: falls back to public feed when followee list is empty")
    void getFeedForUser_emptyFollowees_returnsPublicFeed() {
        Page<Post> page = new PageImpl<>(List.of(activePost()));
        when(postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                eq(Post.Visibility.PUBLIC), any(PageRequest.class))).thenReturn(page);

        Page<PostDto> result = postService.getFeedForUser(List.of(), 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }
}
