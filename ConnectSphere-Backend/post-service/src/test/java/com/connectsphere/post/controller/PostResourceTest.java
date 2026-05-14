package com.connectsphere.post.controller;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.PostDto;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PostResource MockMvc Tests")
class PostResourceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PostService postService;

    private PostDto buildDto() {
        return PostDto.builder()
                .postId("p1")
                .authorId("u1")
                .content("Hello world")
                .visibility(Post.Visibility.PUBLIC)
                .postType(Post.PostType.TEXT)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/posts creates a post and normalizes user id header")
    void createPost_success() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello world");
        request.setVisibility(Post.Visibility.PUBLIC);

        Mockito.when(postService.createPost(eq("u1"), any(CreatePostRequest.class))).thenReturn(buildDto());

        mockMvc.perform(post("/api/v1/posts")
                        .header("X-User-Id", "u1, ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value("p1"))
                .andExpect(jsonPath("$.authorId").value("u1"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/public returns the public feed")
    void getPublicFeed_success() throws Exception {
        Mockito.when(postService.getPublicFeed(0, 20)).thenReturn(new PageImpl<>(List.of(buildDto())));

        mockMvc.perform(get("/api/v1/posts/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value("p1"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/home returns the personalized home feed")
    void getHomeFeed_success() throws Exception {
        Mockito.when(postService.getHomeFeed("u1", 0, 20)).thenReturn(new PageImpl<>(List.of(buildDto())));

        mockMvc.perform(get("/api/v1/posts/home").header("X-User-Id", "u1, duplicated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorId").value("u1"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/feed splits followee ids")
    void getFeed_success() throws Exception {
        Mockito.when(postService.getFeedForUser(eq(List.of("u2", "u3")), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(buildDto())));

        mockMvc.perform(get("/api/v1/posts/feed")
                        .header("X-User-Id", "u1")
                        .param("followeeIds", "u2,u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value("p1"));
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId} updates a post")
    void updatePost_success() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Updated");
        request.setVisibility(Post.Visibility.FOLLOWERS);
        PostDto response = buildDto();
        response.setContent("Updated");

        Mockito.when(postService.updatePost(eq("p1"), eq("u1"), any(CreatePostRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/posts/p1")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"));
    }

    @Test
    @DisplayName("PATCH /api/v1/posts/{postId}/visibility changes post visibility")
    void changeVisibility_success() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/p1/visibility")
                        .header("X-User-Id", "u1")
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Visibility updated"));

        Mockito.verify(postService).changeVisibility("p1", "u1", Post.Visibility.PRIVATE);
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{postId} deletes the post")
    void deletePost_success() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/p1")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post deleted"));

        Mockito.verify(postService).deletePost("p1", "u1", "ADMIN");
    }

    @Test
    @DisplayName("PATCH internal endpoints update counters")
    void internalCounters_success() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/p1/likes/increment")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/posts/p1/likes/decrement")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/posts/p1/comments/increment")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/posts/p1/comments/decrement")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/posts/p1/shares/increment")).andExpect(status().isOk());

        Mockito.verify(postService).incrementLikes("p1");
        Mockito.verify(postService).decrementLikes("p1");
        Mockito.verify(postService).incrementComments("p1");
        Mockito.verify(postService).decrementComments("p1");
        Mockito.verify(postService).incrementShares("p1");
    }

    @Test
    @DisplayName("GET admin stats returns total post count")
    void getAdminStats_success() throws Exception {
        Mockito.when(postService.getTotalPostCount()).thenReturn(42L);

        mockMvc.perform(get("/api/v1/posts/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPosts").value(42));
    }
}
