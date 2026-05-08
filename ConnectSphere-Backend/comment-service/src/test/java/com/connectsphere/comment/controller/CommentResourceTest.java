package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.CommentDto;
import com.connectsphere.comment.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentResource Tests")
class CommentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentServiceImpl commentService;

    private CommentDto mockDto(String id, String content) {
        return CommentDto.builder()
                .commentId(id)
                .postId("p1")
                .authorId("u1")
                .content(content)
                .likesCount(0)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/comments")
    void addComment() throws Exception {
        CommentDto dto = mockDto("c1", "Test comment");
        when(commentService.addComment("p1", "u1", "Test comment", null)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/comments")
                .header("X-User-Id", "u1")
                .param("postId", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Test comment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value("c1"));
    }

    @Test
    @DisplayName("POST /api/v1/comments/{commentId}/reply")
    void reply() throws Exception {
        CommentDto parent = mockDto("parent1", "Parent");
        when(commentService.getCommentById("parent1")).thenReturn(parent);

        CommentDto replyDto = mockDto("reply1", "Reply comment");
        replyDto.setParentCommentId("parent1");
        when(commentService.addComment("p1", "u1", "Reply comment", "parent1")).thenReturn(replyDto);

        mockMvc.perform(post("/api/v1/comments/parent1/reply")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Reply comment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value("reply1"));
    }

    @Test
    @DisplayName("GET /api/v1/comments/post/{postId}")
    void getComments() throws Exception {
        when(commentService.getCommentsByPost(eq("p1"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(mockDto("c1", "c"))));

        mockMvc.perform(get("/api/v1/comments/post/p1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value("c1"));
    }

    @Test
    @DisplayName("GET /api/v1/comments/{commentId}/replies")
    void getReplies() throws Exception {
        when(commentService.getReplies("parent1")).thenReturn(List.of(mockDto("reply1", "r")));

        mockMvc.perform(get("/api/v1/comments/parent1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value("reply1"));
    }

    @Test
    @DisplayName("GET /api/v1/comments/{commentId}")
    void getComment() throws Exception {
        when(commentService.getCommentById("c1")).thenReturn(mockDto("c1", "c"));

        mockMvc.perform(get("/api/v1/comments/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value("c1"));
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{commentId}")
    void updateComment() throws Exception {
        when(commentService.updateComment("c1", "u1", "Updated")).thenReturn(mockDto("c1", "Updated"));

        mockMvc.perform(put("/api/v1/comments/c1")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{commentId}")
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/c1")
                .header("X-User-Id", "u1")
                .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment deleted"));

        verify(commentService).deleteComment("c1", "u1", "USER");
    }

    @Test
    @DisplayName("POST /api/v1/comments/{commentId}/like")
    void likeComment() throws Exception {
        mockMvc.perform(post("/api/v1/comments/c1/like"))
                .andExpect(status().isOk());
        verify(commentService).likeComment("c1");
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{commentId}/like")
    void unlikeComment() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/c1/like"))
                .andExpect(status().isOk());
        verify(commentService).unlikeComment("c1");
    }

    @Test
    @DisplayName("PATCH /api/v1/comments/{commentId}/likes/increment")
    void incrementLikes() throws Exception {
        mockMvc.perform(patch("/api/v1/comments/c1/likes/increment"))
                .andExpect(status().isOk());
        verify(commentService).likeComment("c1");
    }

    @Test
    @DisplayName("PATCH /api/v1/comments/{commentId}/likes/decrement")
    void decrementLikes() throws Exception {
        mockMvc.perform(patch("/api/v1/comments/c1/likes/decrement"))
                .andExpect(status().isOk());
        verify(commentService).unlikeComment("c1");
    }

    @Test
    @DisplayName("GET /api/v1/comments/count/{postId}")
    void count() throws Exception {
        when(commentService.getCommentCount("p1")).thenReturn(42L);

        mockMvc.perform(get("/api/v1/comments/count/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(42));
    }

    @Test
    @DisplayName("GET /api/v1/comments/admin/stats")
    void getAdminStats() throws Exception {
        when(commentService.getTotalCommentCount()).thenReturn(999L);

        mockMvc.perform(get("/api/v1/comments/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComments").value(999));
    }
}
