package com.connectsphere.search.controller;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SearchResource {

    private final SearchService searchService;

    @Tag(name = "Hashtags")
    @Operation(summary = "Get trending hashtags (PUBLIC)")
    @GetMapping("/api/v1/hashtags/trending")
    public ResponseEntity<List<Hashtag>> getTrending(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.getTrendingHashtags(limit));
    }

    @Operation(summary = "Search hashtags by query")
    @GetMapping("/api/v1/hashtags/search")
    public ResponseEntity<List<Hashtag>> searchHashtags(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchHashtags(query));
    }

    @Operation(summary = "Get posts by hashtag")
    @GetMapping("/api/v1/hashtags/{tag}/posts")
    public ResponseEntity<List<String>> getPostsByHashtag(@PathVariable String tag) {
        return ResponseEntity.ok(searchService.getPostsByHashtag(tag));
    }

    @Operation(summary = "Get hashtags for a specific post")
    @GetMapping("/api/v1/hashtags/post/{postId}")
    public ResponseEntity<List<String>> getHashtagsForPost(@PathVariable String postId) {
        return ResponseEntity.ok(searchService.getHashtagsForPost(postId));
    }

    @Operation(summary = "Get total hashtag count")
    @GetMapping("/api/v1/hashtags/count")
    public ResponseEntity<Map<String, Long>> getHashtagCount() {
        return ResponseEntity.ok(Map.of("count", searchService.getHashtagCount()));
    }

    // Internal — called from post events
    @Tag(name = "Search")
    @Operation(summary = "Index a post for hashtags (internal)")
    @PostMapping("/api/v1/search/index")
    public ResponseEntity<Void> index(@RequestBody Map<String, String> body) {
        searchService.indexPost(body.get("postId"), body.get("content"));
        return ResponseEntity.ok().build();
    }
}
