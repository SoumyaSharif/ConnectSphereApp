package com.connectsphere.follow.controller;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.service.impl.FollowServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FollowResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FollowResource Tests")
class FollowResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowServiceImpl followService;

    private static final String FOLLOWER_ID = "u1";
    private static final String FOLLOWEE_ID = "u2";

    @Test
    @DisplayName("POST /api/v1/follows - follow user")
    void followUser() throws Exception {
        Follow follow = Follow.builder().followerId(FOLLOWER_ID).followeeId(FOLLOWEE_ID).build();
        when(followService.follow(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(follow);

        mockMvc.perform(post("/api/v1/follows")
                .header("X-User-Id", FOLLOWER_ID)
                .param("followeeId", FOLLOWEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerId").value(FOLLOWER_ID))
                .andExpect(jsonPath("$.followeeId").value(FOLLOWEE_ID));
    }

    @Test
    @DisplayName("DELETE /api/v1/follows - unfollow user")
    void unfollowUser() throws Exception {
        mockMvc.perform(delete("/api/v1/follows")
                .header("X-User-Id", FOLLOWER_ID)
                .param("followeeId", FOLLOWEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unfollowed"));

        verify(followService).unfollow(FOLLOWER_ID, FOLLOWEE_ID);
    }

    @Test
    @DisplayName("GET /api/v1/follows/is-following")
    void isFollowing() throws Exception {
        when(followService.isFollowing(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(true);

        mockMvc.perform(get("/api/v1/follows/is-following")
                .header("X-User-Id", FOLLOWER_ID)
                .param("followeeId", FOLLOWEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFollowing").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/follows/{userId}/followers")
    void getFollowers() throws Exception {
        when(followService.getFollowerIds("user1")).thenReturn(List.of("f1", "f2"));

        mockMvc.perform(get("/api/v1/follows/user1/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("f1"))
                .andExpect(jsonPath("$[1]").value("f2"));
    }

    @Test
    @DisplayName("GET /api/v1/follows/{userId}/following")
    void getFollowing() throws Exception {
        when(followService.getFollowingIds("user1")).thenReturn(List.of("f1", "f2"));

        mockMvc.perform(get("/api/v1/follows/user1/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("f1"))
                .andExpect(jsonPath("$[1]").value("f2"));
    }

    @Test
    @DisplayName("GET /api/v1/follows/{userId}/followers/count")
    void getFollowerCount() throws Exception {
        when(followService.getFollowerCount("user1")).thenReturn(10L);

        mockMvc.perform(get("/api/v1/follows/user1/followers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/follows/{userId}/following/count")
    void getFollowingCount() throws Exception {
        when(followService.getFollowingCount("user1")).thenReturn(5L);

        mockMvc.perform(get("/api/v1/follows/user1/following/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/follows/{userId}/mutual")
    void getMutual() throws Exception {
        when(followService.getMutualFollows("user1")).thenReturn(List.of("m1"));

        mockMvc.perform(get("/api/v1/follows/user1/mutual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("m1"));
    }

    @Test
    @DisplayName("GET /api/v1/follows/suggestions")
    void getSuggestions() throws Exception {
        when(followService.getSuggestedUsers("user1")).thenReturn(List.of("s1"));

        mockMvc.perform(get("/api/v1/follows/suggestions")
                .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("s1"));
    }

    @Test
    @DisplayName("GET /api/v1/follows/admin/stats")
    void getAdminStats() throws Exception {
        when(followService.getTotalFollowCount()).thenReturn(100L);

        mockMvc.perform(get("/api/v1/follows/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFollows").value(100));
    }
}
