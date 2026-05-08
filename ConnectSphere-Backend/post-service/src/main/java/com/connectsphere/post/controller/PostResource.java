package com.connectsphere.post.controller;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.PostDto;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post CRUD, feed, and search")
public class PostResource {

    private final PostService postService;

    private String normalizeIdentityHeader(String value) {
        if (value == null) {
            return null;
        }

        return value.split(",")[0].trim();
    }

    @Operation(summary = "Create a new post (USER)")
    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(normalizeIdentityHeader(userId), request));
    }

    @Operation(summary = "Get public feed (PUBLIC)")
    @GetMapping("/public")
    public ResponseEntity<Page<PostDto>> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getPublicFeed(page, size));
    }

    @Operation(summary = "Get home feed with public posts plus the current user's posts")
    @GetMapping("/home")
    public ResponseEntity<Page<PostDto>> getHomeFeed(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getHomeFeed(normalizeIdentityHeader(userId), page, size));
    }

    @Operation(summary = "Get personalized news feed (USER)")
    @GetMapping("/feed")
    public ResponseEntity<Page<PostDto>> getFeed(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String followeeIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<String> ids = Arrays.asList(followeeIds.split(","));
        return ResponseEntity.ok(postService.getFeedForUser(ids, page, size));
    }

    @Operation(summary = "Get post by ID")
    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable String postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @Operation(summary = "Get posts by user")
    @GetMapping("/user/{authorId}")
    public ResponseEntity<Page<PostDto>> getPostsByUser(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getPostsByUser(authorId, page, size));
    }

    @Operation(summary = "Search posts by content")
    @GetMapping("/search")
    public ResponseEntity<Page<PostDto>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.searchPosts(query, page, size));
    }

    @Operation(summary = "Update a post (USER/ADMIN)")
    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.ok(postService.updatePost(postId, normalizeIdentityHeader(userId), request));
    }

    @Operation(summary = "Change post visibility (USER)")
    @PatchMapping("/{postId}/visibility")
    public ResponseEntity<Map<String, String>> changeVisibility(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam Post.Visibility visibility) {
        postService.changeVisibility(postId, normalizeIdentityHeader(userId), visibility);
        return ResponseEntity.ok(Map.of("message", "Visibility updated"));
    }

    @Operation(summary = "Delete a post (USER/ADMIN)")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        postService.deletePost(postId, normalizeIdentityHeader(userId), role);
        return ResponseEntity.ok(Map.of("message", "Post deleted"));
    }

    @Operation(summary = "Get post count for a user")
    @GetMapping("/count/{authorId}")
    public ResponseEntity<Map<String, Long>> getPostCount(@PathVariable String authorId) {
        return ResponseEntity.ok(Map.of("count", postService.getPostCount(authorId)));
    }

    // Internal endpoints called by other services
    @PatchMapping("/{postId}/likes/increment")
    public ResponseEntity<Void> incrementLikes(@PathVariable String postId) {
        postService.incrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{postId}/likes/decrement")
    public ResponseEntity<Void> decrementLikes(@PathVariable String postId) {
        postService.decrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{postId}/comments/increment")
    public ResponseEntity<Void> incrementComments(@PathVariable String postId) {
        postService.incrementComments(postId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{postId}/comments/decrement")
    public ResponseEntity<Void> decrementComments(@PathVariable String postId) {
        postService.decrementComments(postId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{postId}/shares/increment")
    public ResponseEntity<Void> incrementShares(@PathVariable String postId) {
        postService.incrementShares(postId);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/posts/admin/stats
     * Returns platform-wide post count. Called internally by auth-service admin dashboard.
     * The gateway JWT filter ensures only authenticated requests with a valid token reach this.
     */
    @Operation(summary = "Get total post count for admin dashboard")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        long total = postService.getTotalPostCount();
        return ResponseEntity.ok(Map.of("totalPosts", total));
    }
}

