package com.connectsphere.follow.service;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowServiceImpl Tests")
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private FollowServiceImpl followService;

    private static final String FOLLOWER_ID = "user-1";
    private static final String FOLLOWEE_ID = "user-2";

    // ─── follow() ───────────────────────────────────────────────
    @Test
    @DisplayName("follow: happy path saves follow entity")
    void follow_success() {
        when(followRepository.existsByFollowerIdAndFolloweeId(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(false);
        Follow saved = Follow.builder()
                .followerId(FOLLOWER_ID).followeeId(FOLLOWEE_ID)
                .status(Follow.FollowStatus.ACTIVE).build();
        when(followRepository.save(any(Follow.class))).thenReturn(saved);

        Follow result = followService.follow(FOLLOWER_ID, FOLLOWEE_ID);

        assertThat(result.getFollowerId()).isEqualTo(FOLLOWER_ID);
        assertThat(result.getFolloweeId()).isEqualTo(FOLLOWEE_ID);
        assertThat(result.getStatus()).isEqualTo(Follow.FollowStatus.ACTIVE);
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    @DisplayName("follow: throws when trying to follow yourself")
    void follow_selfFollow_throws() {
        assertThatThrownBy(() -> followService.follow(FOLLOWER_ID, FOLLOWER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot follow yourself");

        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("follow: throws when already following")
    void follow_alreadyFollowing_throws() {
        when(followRepository.existsByFollowerIdAndFolloweeId(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> followService.follow(FOLLOWER_ID, FOLLOWEE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Already following");
    }

    // ─── unfollow() ─────────────────────────────────────────────
    @Test
    @DisplayName("unfollow: delegates delete to repository")
    void unfollow_success() {
        followService.unfollow(FOLLOWER_ID, FOLLOWEE_ID);
        verify(followRepository).deleteByFollowerIdAndFolloweeId(FOLLOWER_ID, FOLLOWEE_ID);
    }

    // ─── isFollowing() ──────────────────────────────────────────
    @Test
    @DisplayName("isFollowing: returns true when relationship exists")
    void isFollowing_true() {
        when(followRepository.existsByFollowerIdAndFolloweeId(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(true);
        assertThat(followService.isFollowing(FOLLOWER_ID, FOLLOWEE_ID)).isTrue();
    }

    @Test
    @DisplayName("isFollowing: returns false when no relationship")
    void isFollowing_false() {
        when(followRepository.existsByFollowerIdAndFolloweeId(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(false);
        assertThat(followService.isFollowing(FOLLOWER_ID, FOLLOWEE_ID)).isFalse();
    }

    // ─── counts ─────────────────────────────────────────────────
    @Test
    @DisplayName("getFollowerCount: returns value from repository")
    void getFollowerCount() {
        when(followRepository.countByFolloweeIdAndStatus(FOLLOWEE_ID, Follow.FollowStatus.ACTIVE)).thenReturn(5L);
        assertThat(followService.getFollowerCount(FOLLOWEE_ID)).isEqualTo(5L);
    }

    @Test
    @DisplayName("getFollowingCount: returns value from repository")
    void getFollowingCount() {
        when(followRepository.countByFollowerIdAndStatus(FOLLOWER_ID, Follow.FollowStatus.ACTIVE)).thenReturn(3L);
        assertThat(followService.getFollowingCount(FOLLOWER_ID)).isEqualTo(3L);
    }

    @Test
    @DisplayName("getTotalFollowCount: returns total active follows")
    void getTotalFollowCount() {
        when(followRepository.countByStatus(Follow.FollowStatus.ACTIVE)).thenReturn(42L);
        assertThat(followService.getTotalFollowCount()).isEqualTo(42L);
    }

    // ─── lists ──────────────────────────────────────────────────
    @Test
    @DisplayName("getFollowerIds: maps entities to IDs")
    void getFollowerIds() {
        Follow f = Follow.builder().followerId("f1").build();
        when(followRepository.findByFolloweeIdAndStatus(FOLLOWEE_ID, Follow.FollowStatus.ACTIVE)).thenReturn(List.of(f));
        
        List<String> ids = followService.getFollowerIds(FOLLOWEE_ID);
        assertThat(ids).containsExactly("f1");
    }

    @Test
    @DisplayName("getMutualFollows: returns value from repository")
    void getMutualFollows() {
        when(followRepository.findMutualFollows(FOLLOWER_ID)).thenReturn(List.of("mutual-1"));
        assertThat(followService.getMutualFollows(FOLLOWER_ID)).containsExactly("mutual-1");
    }

    // ─── getSuggestedUsers() ─────────────────────────────────────
    @Test
    @DisplayName("getSuggestedUsers: returns empty list when user follows nobody")
    void getSuggestedUsers_noFollowing_empty() {
        when(followRepository.findFolloweeIds(FOLLOWER_ID)).thenReturn(List.of());
        assertThat(followService.getSuggestedUsers(FOLLOWER_ID)).isEmpty();
    }

    @Test
    @DisplayName("getSuggestedUsers: excludes already-followed and self")
    void getSuggestedUsers_excludesSelfAndFollowing() {
        String alreadyFollowing = "user-3";
        when(followRepository.findFolloweeIds(FOLLOWER_ID)).thenReturn(List.of(alreadyFollowing));
        // user-3 follows user-4 — suggestion
        when(followRepository.findFolloweeIds(alreadyFollowing)).thenReturn(List.of("user-4", FOLLOWER_ID));

        List<String> suggestions = followService.getSuggestedUsers(FOLLOWER_ID);
        assertThat(suggestions).containsExactly("user-4");
        assertThat(suggestions).doesNotContain(FOLLOWER_ID);
    }
}
