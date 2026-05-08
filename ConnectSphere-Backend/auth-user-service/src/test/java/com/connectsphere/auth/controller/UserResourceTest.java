package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.UserProfileDto;
import com.connectsphere.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.security.CustomUserDetailsService;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserResource MockMvc Tests")
class UserResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/v1/users/{userId}/profile - Success")
    void getProfile() throws Exception {
        UserProfileDto mockDto = UserProfileDto.builder().userId("u1").username("test").build();
        Mockito.when(authService.getUserById("u1")).thenReturn(mockDto);

        mockMvc.perform(get("/api/v1/users/u1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.username").value("test"));
    }

    @Test
    @DisplayName("GET /api/v1/users/search - Success")
    void search() throws Exception {
        UserProfileDto mockDto = UserProfileDto.builder().userId("u1").username("test").build();
        Mockito.when(authService.searchUsers("test")).thenReturn(List.of(mockDto));

        mockMvc.perform(get("/api/v1/users/search")
                .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"));
    }
}
