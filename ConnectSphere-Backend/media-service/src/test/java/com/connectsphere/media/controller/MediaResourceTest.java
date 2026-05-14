package com.connectsphere.media.controller;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MediaResource Tests")
class MediaResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MediaService mediaService;

    private Media buildMedia() {
        return Media.builder()
                .mediaId("m1")
                .uploaderId("u1")
                .url("https://cdn/image.png")
                .mediaType(Media.MediaType.IMAGE)
                .mimeType("image/png")
                .linkedPostId("p1")
                .build();
    }

    private Story buildStory() {
        return Story.builder()
                .storyId("s1")
                .authorId("u1")
                .mediaUrl("https://cdn/story.png")
                .caption("hello")
                .mediaType(Media.MediaType.IMAGE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/media uploads media metadata")
    void upload_success() throws Exception {
        when(mediaService.uploadMedia("u1", "https://cdn/image.png", Media.MediaType.VIDEO, 120L, "video/mp4", "p1"))
                .thenReturn(buildMedia());

        mockMvc.perform(post("/api/v1/media")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", "https://cdn/image.png",
                                "mediaType", "VIDEO",
                                "sizeKb", "120",
                                "mimeType", "video/mp4",
                                "linkedPostId", "p1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value("m1"));
    }

    @Test
    @DisplayName("GET /api/v1/media/post/{postId} returns media for a post")
    void getByPost_success() throws Exception {
        when(mediaService.getMediaByPost("p1")).thenReturn(List.of(buildMedia()));

        mockMvc.perform(get("/api/v1/media/post/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linkedPostId").value("p1"));
    }

    @Test
    @DisplayName("GET /api/v1/media/{mediaId} returns a media item")
    void getById_success() throws Exception {
        when(mediaService.getMediaById("m1")).thenReturn(buildMedia());

        mockMvc.perform(get("/api/v1/media/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value("m1"));
    }

    @Test
    @DisplayName("DELETE /api/v1/media/{mediaId} deletes media")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/media/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media deleted"));

        verify(mediaService).deleteMedia("m1");
    }

    @Test
    @DisplayName("POST /api/v1/stories creates a story")
    void createStory_success() throws Exception {
        when(mediaService.createStory("u1", "https://cdn/story.png", "hello", Media.MediaType.IMAGE))
                .thenReturn(buildStory());

        mockMvc.perform(post("/api/v1/stories")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mediaUrl", "https://cdn/story.png",
                                "caption", "hello",
                                "mediaType", "IMAGE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storyId").value("s1"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/user/{userId} returns user stories")
    void getStoriesByUser_success() throws Exception {
        when(mediaService.getActiveStoriesByUser("u1")).thenReturn(List.of(buildStory()));

        mockMvc.perform(get("/api/v1/stories/user/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value("u1"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/me returns current user stories")
    void getMyStories_success() throws Exception {
        when(mediaService.getActiveStoriesByUser("u1")).thenReturn(List.of(buildStory()));

        mockMvc.perform(get("/api/v1/stories/me").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storyId").value("s1"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/feed without followees uses feed lookup")
    void getStoriesFeed_withoutFollowees_success() throws Exception {
        when(mediaService.getActiveStoriesFeed("u1")).thenReturn(List.of(buildStory()));

        mockMvc.perform(get("/api/v1/stories/feed").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storyId").value("s1"));
    }

    @Test
    @DisplayName("GET /api/v1/stories/feed with followees splits ids")
    void getStoriesFeed_withFollowees_success() throws Exception {
        when(mediaService.getActiveStoriesByFollowees(List.of("u2", "u3"))).thenReturn(List.of(buildStory()));

        mockMvc.perform(get("/api/v1/stories/feed")
                        .header("X-User-Id", "u1")
                        .param("followeeIds", "u2, u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storyId").value("s1"));
    }

    @Test
    @DisplayName("POST /api/v1/stories/{storyId}/view increments story views")
    void viewStory_success() throws Exception {
        mockMvc.perform(post("/api/v1/stories/s1/view"))
                .andExpect(status().isOk());

        verify(mediaService).viewStory("s1");
    }

    @Test
    @DisplayName("DELETE /api/v1/stories/{storyId} deletes a story")
    void deleteStory_success() throws Exception {
        mockMvc.perform(delete("/api/v1/stories/s1").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story deleted"));

        verify(mediaService).deleteStory("s1", "u1");
    }
}
