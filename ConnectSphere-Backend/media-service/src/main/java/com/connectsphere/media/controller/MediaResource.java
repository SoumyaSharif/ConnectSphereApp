package com.connectsphere.media.controller;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MediaResource {

    private final MediaService mediaService;

    // ---- Media Endpoints ----
    @Tag(name = "Media")
    @Operation(summary = "Upload media metadata (USER)")
    @PostMapping("/api/v1/media")
    public ResponseEntity<Media> upload(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        Media media = mediaService.uploadMedia(userId,
                body.get("url"),
                Media.MediaType.valueOf(body.getOrDefault("mediaType", "IMAGE")),
                Long.parseLong(body.getOrDefault("sizeKb", "0")),
                body.get("mimeType"),
                body.get("linkedPostId"));
        return ResponseEntity.ok(media);
    }

    @Operation(summary = "Get media by post")
    @GetMapping("/api/v1/media/post/{postId}")
    public ResponseEntity<List<Media>> getByPost(@PathVariable String postId) {
        return ResponseEntity.ok(mediaService.getMediaByPost(postId));
    }

    @Operation(summary = "Get media by ID")
    @GetMapping("/api/v1/media/{mediaId}")
    public ResponseEntity<Media> getById(@PathVariable String mediaId) {
        return ResponseEntity.ok(mediaService.getMediaById(mediaId));
    }

    @Operation(summary = "Soft-delete media (USER/ADMIN)")
    @DeleteMapping("/api/v1/media/{mediaId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(Map.of("message", "Media deleted"));
    }

    // ---- Story Endpoints ----
    @Operation(summary = "Create a story (USER)")
    @PostMapping("/api/v1/stories")
    public ResponseEntity<Story> createStory(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        Story story = mediaService.createStory(userId, body.get("mediaUrl"), body.get("caption"),
                Media.MediaType.valueOf(body.getOrDefault("mediaType", "IMAGE")));
        return ResponseEntity.ok(story);
    }

    @Operation(summary = "Get active stories by user")
    @GetMapping("/api/v1/stories/user/{userId}")
    public ResponseEntity<List<Story>> getStoriesByUser(@PathVariable String userId) {
        return ResponseEntity.ok(mediaService.getActiveStoriesByUser(userId));
    }

    @Operation(summary = "Get active stories for current user (USER)")
    @GetMapping("/api/v1/stories/me")
    public ResponseEntity<List<Story>> getMyStories(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(mediaService.getActiveStoriesByUser(userId));
    }

    @Operation(summary = "Get stories from followees (USER)")
    @GetMapping("/api/v1/stories/feed")
    public ResponseEntity<List<Story>> getStoriesFeed(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String followeeIds) {
        if (followeeIds == null || followeeIds.isBlank()) {
            return ResponseEntity.ok(mediaService.getActiveStoriesFeed(userId));
        }

        List<String> ids = Arrays.stream(followeeIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());
        return ResponseEntity.ok(mediaService.getActiveStoriesByFollowees(ids));
    }

    @Operation(summary = "View a story (increments view count)")
    @PostMapping("/api/v1/stories/{storyId}/view")
    public ResponseEntity<Void> viewStory(@PathVariable String storyId) {
        mediaService.viewStory(storyId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a story (USER)")
    @DeleteMapping("/api/v1/stories/{storyId}")
    public ResponseEntity<Map<String, String>> deleteStory(
            @PathVariable String storyId,
            @RequestHeader("X-User-Id") String userId) {
        mediaService.deleteStory(storyId, userId);
        return ResponseEntity.ok(Map.of("message", "Story deleted"));
    }
}
