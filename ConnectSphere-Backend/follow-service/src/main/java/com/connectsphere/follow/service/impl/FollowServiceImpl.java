package com.connectsphere.follow.service.impl;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl {

    private final FollowRepository followRepository;

    @Transactional
    public Follow follow(String followerId, String followeeId) {
        if (followerId.equals(followeeId)) throw new RuntimeException("Cannot follow yourself");
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new RuntimeException("Already following");
        }
        Follow follow = Follow.builder()
                .followerId(followerId).followeeId(followeeId)
                .status(Follow.FollowStatus.ACTIVE).build();
        return followRepository.save(follow);
    }

    @Transactional
    public void unfollow(String followerId, String followeeId) {
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    public boolean isFollowing(String followerId, String followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    public List<String> getFollowerIds(String userId) {
        return followRepository.findByFolloweeIdAndStatus(userId, Follow.FollowStatus.ACTIVE)
                .stream().map(Follow::getFollowerId).collect(Collectors.toList());
    }

    public List<String> getFollowingIds(String userId) {
        return followRepository.findFolloweeIds(userId);
    }

    public long getFollowerCount(String userId) {
        return followRepository.countByFolloweeIdAndStatus(userId, Follow.FollowStatus.ACTIVE);
    }

    public long getFollowingCount(String userId) {
        return followRepository.countByFollowerIdAndStatus(userId, Follow.FollowStatus.ACTIVE);
    }

    public List<String> getMutualFollows(String userId) {
        return followRepository.findMutualFollows(userId);
    }

    public List<String> getSuggestedUsers(String userId) {
        List<String> following = getFollowingIds(userId);
        if (following.isEmpty()) return List.of();
        // Suggest users followed by people I follow, whom I don't follow yet
        return following.stream()
                .flatMap(fId -> followRepository.findFolloweeIds(fId).stream())
                .distinct()
                .filter(id -> !id.equals(userId) && !following.contains(id))
                .limit(10)
                .collect(Collectors.toList());
    }

    public long getTotalFollowCount() {
        return followRepository.countByStatus(Follow.FollowStatus.ACTIVE);
    }
}
