package com.connectsphere.like.service;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeServiceImpl Tests")
class LikeServiceImplTest {

    @Mock private LikeRepository likeRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private LikeServiceImpl likeService;

    private static final String USER_ID  = "u1";
    private static final String TARGET_ID = "post-1";
    private static final Like.TargetType POST = Like.TargetType.POST;

    // ─── likeTarget() ───────────────────────────────────────────
    @Test
    @DisplayName("likeTarget: saves like for new reaction")
    void likeTarget_success() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(false);
        Like saved = Like.builder()
                .userId(USER_ID).targetId(TARGET_ID)
                .targetType(POST).reactionType(Like.ReactionType.LIKE).build();
        when(likeRepository.save(any(Like.class))).thenReturn(saved);

        Like result = likeService.likeTarget(USER_ID, TARGET_ID, POST, Like.ReactionType.LIKE);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTargetId()).isEqualTo(TARGET_ID);
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    @DisplayName("likeTarget: throws when already reacted")
    void likeTarget_alreadyReacted_throws() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(true);

        assertThatThrownBy(() -> likeService.likeTarget(USER_ID, TARGET_ID, POST, Like.ReactionType.LIKE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Already reacted");
    }

    // ─── unlikeTarget() ─────────────────────────────────────────
    @Test
    @DisplayName("unlikeTarget: deletes existing reaction")
    void unlikeTarget_success() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(true);

        likeService.unlikeTarget(USER_ID, TARGET_ID, POST);

        verify(likeRepository).deleteByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST);
    }

    @Test
    @DisplayName("unlikeTarget: throws when no reaction exists")
    void unlikeTarget_noReaction_throws() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(false);

        assertThatThrownBy(() -> likeService.unlikeTarget(USER_ID, TARGET_ID, POST))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No reaction found");
    }

    // ─── changeReaction() ───────────────────────────────────────
    @Test
    @DisplayName("changeReaction: updates reaction type")
    void changeReaction_success() {
        Like existing = Like.builder()
                .userId(USER_ID).targetId(TARGET_ID)
                .targetType(POST).reactionType(Like.ReactionType.LIKE).build();
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST))
                .thenReturn(Optional.of(existing));
        when(likeRepository.save(existing)).thenReturn(existing);

        Like result = likeService.changeReaction(USER_ID, TARGET_ID, POST, Like.ReactionType.LOVE);

        assertThat(result.getReactionType()).isEqualTo(Like.ReactionType.LOVE);
    }

    @Test
    @DisplayName("changeReaction: throws when no existing reaction")
    void changeReaction_notFound_throws() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.changeReaction(USER_ID, TARGET_ID, POST, Like.ReactionType.LOVE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No reaction found");
    }

    // ─── hasLiked() ─────────────────────────────────────────────
    @Test
    @DisplayName("hasLiked: delegates to repository")
    void hasLiked_returnsRepositoryResult() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(true);
        assertThat(likeService.hasLiked(USER_ID, TARGET_ID, POST)).isTrue();
    }

    // ─── counts ─────────────────────────────────────────────────
    @Test
    @DisplayName("getLikeCount: returns count from repository")
    void getLikeCount() {
        when(likeRepository.countByTargetIdAndTargetType(TARGET_ID, POST)).thenReturn(7L);
        assertThat(likeService.getLikeCount(TARGET_ID, POST)).isEqualTo(7L);
    }

    @Test
    @DisplayName("getTotalLikeCount: returns all likes count")
    void getTotalLikeCount() {
        when(likeRepository.count()).thenReturn(100L);
        assertThat(likeService.getTotalLikeCount()).isEqualTo(100L);
    }

    // ─── getLikesByUser() ────────────────────────────────────────
    @Test
    @DisplayName("getLikesByUser: returns list from repository")
    void getLikesByUser() {
        List<Like> likes = List.of(Like.builder().userId(USER_ID).build());
        when(likeRepository.findByUserId(USER_ID)).thenReturn(likes);
        assertThat(likeService.getLikesByUser(USER_ID)).hasSize(1);
    }

    @Test
    @DisplayName("getReactionSummary groups repository rows into a map")
    void getReactionSummary() {
        when(likeRepository.countByTargetIdAndTargetTypeGroupByReactionType(TARGET_ID, POST))
                .thenReturn(List.of(new Object[]{"LIKE", 2L}, new Object[]{"LOVE", 1L}));

        assertThat(likeService.getReactionSummary(TARGET_ID, POST))
                .isEqualTo(Map.of("LIKE", 2L, "LOVE", 1L));
    }

    @Test
    @DisplayName("getLikesByTarget delegates to repository")
    void getLikesByTarget() {
        List<Like> likes = List.of(Like.builder().targetId(TARGET_ID).build());
        when(likeRepository.findByTargetIdAndTargetType(TARGET_ID, POST)).thenReturn(likes);

        assertThat(likeService.getLikesByTarget(TARGET_ID, POST)).isSameAs(likes);
    }

    @Test
    @DisplayName("likeTarget for comments creates comment notification when target belongs to another user")
    void likeTarget_commentNotification() {
        String commentId = "c1";
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, commentId, Like.TargetType.COMMENT)).thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.getForObject("http://comment-service/api/v1/comments/{id}", Map.class, commentId))
                .thenReturn(Map.of("authorId", "u2", "postId", "p9"));

        likeService.likeTarget(USER_ID, commentId, Like.TargetType.COMMENT, Like.ReactionType.LIKE);

        verify(restTemplate).patchForObject("http://comment-service/api/v1/comments/{id}/likes/increment", null, Void.class, commentId);
        verify(restTemplate).postForObject(
                eq("http://notification-service/api/v1/notifications/internal"),
                argThat(payload -> {
                    if (!(payload instanceof Map<?, ?> map)) {
                        return false;
                    }
                    return "u2".equals(map.get("recipientId"))
                            && "COMMENT".equals(map.get("targetType"))
                            && "/post/p9".equals(map.get("deepLinkUrl"));
                }),
                eq(Void.class)
        );
    }

    @Test
    @DisplayName("likeTarget skips notification when user likes their own post")
    void likeTarget_ownPostDoesNotNotify() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TARGET_ID, POST)).thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.getForObject("http://post-service/api/v1/posts/{id}", Map.class, TARGET_ID))
                .thenReturn(Map.of("authorId", USER_ID));

        likeService.likeTarget(USER_ID, TARGET_ID, POST, Like.ReactionType.LIKE);

        verify(restTemplate, never()).postForObject(eq("http://notification-service/api/v1/notifications/internal"), any(), eq(Void.class));
    }
}
