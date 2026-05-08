package com.connectsphere.follow.controller;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.service.impl.FollowServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@Tag(name = "Follows", description = "Social graph — follow/unfollow, suggestions")
public class FollowResource {

    private final FollowServiceImpl followService;

    @Operation(summary = "Follow a user (USER)")
    @PostMapping({"", "/"})
    public ResponseEntity<Follow> follow(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String followeeId) {
        return ResponseEntity.ok(followService.follow(userId, followeeId));
    }

    @Operation(summary = "Unfollow a user (USER)")
    @DeleteMapping({"", "/"})
    public ResponseEntity<Map<String, String>> unfollow(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String followeeId) {
        followService.unfollow(userId, followeeId);
        return ResponseEntity.ok(Map.of("message", "Unfollowed"));
    }

    @Operation(summary = "Check if following")
    @GetMapping("/is-following")
    public ResponseEntity<Map<String, Boolean>> isFollowing(
            @RequestHeader("X-User-Id") String followerId,
            @RequestParam String followeeId) {
        return ResponseEntity.ok(Map.of("isFollowing", followService.isFollowing(followerId, followeeId)));
    }

    @Operation(summary = "Get follower IDs of a user")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<String>> getFollowers(@PathVariable String userId) {
        return ResponseEntity.ok(followService.getFollowerIds(userId));
    }

    @Operation(summary = "Get following IDs of a user")
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<String>> getFollowing(@PathVariable String userId) {
        return ResponseEntity.ok(followService.getFollowingIds(userId));
    }

    @Operation(summary = "Get follower count")
    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<Map<String, Long>> getFollowerCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", followService.getFollowerCount(userId)));
    }

    @Operation(summary = "Get following count")
    @GetMapping("/{userId}/following/count")
    public ResponseEntity<Map<String, Long>> getFollowingCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", followService.getFollowingCount(userId)));
    }

    @Operation(summary = "Get mutual connections")
    @GetMapping("/{userId}/mutual")
    public ResponseEntity<List<String>> getMutual(@PathVariable String userId) {
        return ResponseEntity.ok(followService.getMutualFollows(userId));
    }

    @Operation(summary = "Get suggested users to follow (USER)")
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(followService.getSuggestedUsers(userId));
    }

    @Operation(summary = "Get total follow relationship count for admin dashboard")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(Map.of("totalFollows", followService.getTotalFollowCount()));
    }
}
