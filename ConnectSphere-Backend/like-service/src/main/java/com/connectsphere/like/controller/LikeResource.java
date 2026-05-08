package com.connectsphere.like.controller;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.service.impl.LikeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Polymorphic reactions on posts and comments")
public class LikeResource {

    private final LikeServiceImpl likeService;

    @Operation(summary = "React to a post or comment (USER)")
    @PostMapping
    public ResponseEntity<?> like(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType,
            @RequestParam(defaultValue = "LIKE") Like.ReactionType reactionType) {
        return ResponseEntity.ok(likeService.likeTarget(userId, targetId, targetType, reactionType));
    }

    @Operation(summary = "Remove reaction (USER)")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlike(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType) {
        likeService.unlikeTarget(userId, targetId, targetType);
        return ResponseEntity.ok(Map.of("message", "Reaction removed"));
    }

    @Operation(summary = "Change reaction type (USER)")
    @PutMapping
    public ResponseEntity<?> changeReaction(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType,
            @RequestParam Like.ReactionType reactionType) {
        return ResponseEntity.ok(likeService.changeReaction(userId, targetId, targetType, reactionType));
    }

    @Operation(summary = "Check if user has reacted")
    @GetMapping("/has-liked")
    public ResponseEntity<Map<String, Boolean>> hasLiked(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType) {
        return ResponseEntity.ok(Map.of("hasLiked", likeService.hasLiked(userId, targetId, targetType)));
    }

    @Operation(summary = "Get reaction count for a target")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount(
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType) {
        return ResponseEntity.ok(Map.of("count", likeService.getLikeCount(targetId, targetType)));
    }

    @Operation(summary = "Get reaction summary (emoji breakdown)")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary(
            @RequestParam String targetId,
            @RequestParam Like.TargetType targetType) {
        return ResponseEntity.ok(likeService.getReactionSummary(targetId, targetType));
    }

    @Operation(summary = "Get all likes by a user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLikesByUser(@PathVariable String userId) {
        return ResponseEntity.ok(likeService.getLikesByUser(userId));
    }

    @Operation(summary = "Get total like count for admin dashboard")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(Map.of("totalLikes", likeService.getTotalLikeCount()));
    }
}
