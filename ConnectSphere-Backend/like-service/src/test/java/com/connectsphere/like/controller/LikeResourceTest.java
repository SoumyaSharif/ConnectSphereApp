package com.connectsphere.like.controller;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LikeResource Tests")
class LikeResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LikeServiceImpl likeService;

    private Like buildLike() {
        return Like.builder()
                .likeId("l1")
                .userId("u1")
                .targetId("p1")
                .targetType(Like.TargetType.POST)
                .reactionType(Like.ReactionType.LIKE)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/likes creates a reaction")
    void like_success() throws Exception {
        when(likeService.likeTarget("u1", "p1", Like.TargetType.POST, Like.ReactionType.LOVE))
                .thenReturn(buildLike());

        mockMvc.perform(post("/api/v1/likes")
                        .header("X-User-Id", "u1")
                        .param("targetId", "p1")
                        .param("targetType", "POST")
                        .param("reactionType", "LOVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeId").value("l1"))
                .andExpect(jsonPath("$.targetId").value("p1"));
    }

    @Test
    @DisplayName("DELETE /api/v1/likes removes a reaction")
    void unlike_success() throws Exception {
        mockMvc.perform(delete("/api/v1/likes")
                        .header("X-User-Id", "u1")
                        .param("targetId", "p1")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction removed"));

        verify(likeService).unlikeTarget("u1", "p1", Like.TargetType.POST);
    }

    @Test
    @DisplayName("PUT /api/v1/likes changes a reaction type")
    void changeReaction_success() throws Exception {
        Like changed = buildLike();
        changed.setReactionType(Like.ReactionType.WOW);
        when(likeService.changeReaction("u1", "p1", Like.TargetType.POST, Like.ReactionType.WOW))
                .thenReturn(changed);

        mockMvc.perform(put("/api/v1/likes")
                        .header("X-User-Id", "u1")
                        .param("targetId", "p1")
                        .param("targetType", "POST")
                        .param("reactionType", "WOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("WOW"));
    }

    @Test
    @DisplayName("GET /api/v1/likes/has-liked returns reaction status")
    void hasLiked_success() throws Exception {
        when(likeService.hasLiked("u1", "p1", Like.TargetType.POST)).thenReturn(true);

        mockMvc.perform(get("/api/v1/likes/has-liked")
                        .header("X-User-Id", "u1")
                        .param("targetId", "p1")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasLiked").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/likes/count returns like count")
    void getCount_success() throws Exception {
        when(likeService.getLikeCount("p1", Like.TargetType.POST)).thenReturn(8L);

        mockMvc.perform(get("/api/v1/likes/count")
                        .param("targetId", "p1")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(8));
    }

    @Test
    @DisplayName("GET /api/v1/likes/summary returns reaction summary")
    void getSummary_success() throws Exception {
        when(likeService.getReactionSummary("p1", Like.TargetType.POST))
                .thenReturn(Map.of("LIKE", 3L, "LOVE", 2L));

        mockMvc.perform(get("/api/v1/likes/summary")
                        .param("targetId", "p1")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.LIKE").value(3))
                .andExpect(jsonPath("$.LOVE").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/likes/user/{userId} returns likes by user")
    void getLikesByUser_success() throws Exception {
        when(likeService.getLikesByUser("u1")).thenReturn(List.of(buildLike()));

        mockMvc.perform(get("/api/v1/likes/user/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"));
    }

    @Test
    @DisplayName("GET /api/v1/likes/admin/stats returns total count")
    void getAdminStats_success() throws Exception {
        when(likeService.getTotalLikeCount()).thenReturn(15L);

        mockMvc.perform(get("/api/v1/likes/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikes").value(15));
    }
}
