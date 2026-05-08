package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.CommentDto;
import com.connectsphere.comment.service.impl.CommentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Threaded comments and replies")
public class CommentResource {

    private final CommentServiceImpl commentService;

    @Operation(summary = "Add top-level comment (USER)")
    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String postId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(postId, userId, body.get("content"), null));
    }

    @Operation(summary = "Reply to a comment (USER)")
    @PostMapping("/{commentId}/reply")
    public ResponseEntity<CommentDto> reply(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {
        CommentDto parent = commentService.getCommentById(commentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(parent.getPostId(), userId, body.get("content"), commentId));
    }

    @Operation(summary = "Get comments for a post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentDto>> getComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, page, size));
    }

    @Operation(summary = "Get replies to a comment")
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDto>> getReplies(@PathVariable String commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @Operation(summary = "Get comment by ID")
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getComment(@PathVariable String commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    @Operation(summary = "Update a comment (USER)")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> update(
            @PathVariable String commentId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(commentService.updateComment(commentId, userId, body.get("content")));
    }

    @Operation(summary = "Delete a comment (USER/ADMIN)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String commentId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        commentService.deleteComment(commentId, userId, role);
        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }

    @Operation(summary = "Like a comment")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(@PathVariable String commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unlike a comment")
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(@PathVariable String commentId) {
        commentService.unlikeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{commentId}/likes/increment")
    public ResponseEntity<Void> incrementLikes(@PathVariable String commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{commentId}/likes/decrement")
    public ResponseEntity<Void> decrementLikes(@PathVariable String commentId) {
        commentService.unlikeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get comment count for a post")
    @GetMapping("/count/{postId}")
    public ResponseEntity<Map<String, Long>> count(@PathVariable String postId) {
        return ResponseEntity.ok(Map.of("count", commentService.getCommentCount(postId)));
    }

    @Operation(summary = "Get total comment count for admin dashboard")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(Map.of("totalComments", commentService.getTotalCommentCount()));
    }
}
