package com.connectsphere.search.controller;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SearchResource Tests")
class SearchResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    private Hashtag buildHashtag() {
        return Hashtag.builder()
                .hashtagId("h1")
                .tag("space")
                .postCount(12)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/hashtags/trending returns trending hashtags")
    void getTrending_success() throws Exception {
        when(searchService.getTrendingHashtags(10)).thenReturn(List.of(buildHashtag()));

        mockMvc.perform(get("/api/v1/hashtags/trending").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tag").value("space"));
    }

    @Test
    @DisplayName("GET /api/v1/hashtags/search searches hashtags")
    void searchHashtags_success() throws Exception {
        when(searchService.searchHashtags("spa")).thenReturn(List.of(buildHashtag()));

        mockMvc.perform(get("/api/v1/hashtags/search").param("query", "spa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hashtagId").value("h1"));
    }

    @Test
    @DisplayName("GET /api/v1/hashtags/{tag}/posts returns posts for a tag")
    void getPostsByHashtag_success() throws Exception {
        when(searchService.getPostsByHashtag("space")).thenReturn(List.of("p1", "p2"));

        mockMvc.perform(get("/api/v1/hashtags/space/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("p1"));
    }

    @Test
    @DisplayName("GET /api/v1/hashtags/post/{postId} returns hashtags for a post")
    void getHashtagsForPost_success() throws Exception {
        when(searchService.getHashtagsForPost("p1")).thenReturn(List.of("space", "cosmos"));

        mockMvc.perform(get("/api/v1/hashtags/post/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("cosmos"));
    }

    @Test
    @DisplayName("GET /api/v1/hashtags/count returns total hashtag count")
    void getHashtagCount_success() throws Exception {
        when(searchService.getHashtagCount()).thenReturn(9L);

        mockMvc.perform(get("/api/v1/hashtags/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(9));
    }

    @Test
    @DisplayName("POST /api/v1/search/index indexes a post")
    void index_success() throws Exception {
        mockMvc.perform(post("/api/v1/search/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postId", "p1",
                                "content", "hello #space"
                        ))))
                .andExpect(status().isOk());

        verify(searchService).indexPost("p1", "hello #space");
    }
}
