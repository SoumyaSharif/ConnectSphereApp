package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthResource.class)
@AutoConfigureMockMvc(addFilters = false) // Disable spring security for pure controller unit test
@DisplayName("AuthResource MockMvc Tests")
class AuthResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/v1/auth/register - Success")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFullName("Test User");

        AuthResponse response = AuthResponse.builder().accessToken("token").build();

        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Validation Failure")
    void register_validationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("te"); // Too short
        request.setEmail("invalid-email");
        request.setPassword("short");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        AuthResponse response = AuthResponse.builder().accessToken("token").build();
        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    @DisplayName("PUT /api/v1/auth/profile - Success")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        UserProfileDto profile = UserProfileDto.builder().userId("u1").fullName("Updated Name").build();
        Mockito.when(authService.updateProfile(eq("u1"), any(UpdateProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(put("/api/v1/auth/profile")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/password - Success")
    void changePassword_success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        mockMvc.perform(put("/api/v1/auth/password")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
        
        Mockito.verify(authService).changePassword(eq("u1"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Success")
    void forgotPassword_success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Success")
    void resetPassword_success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");
        request.setNewPassword("NewPass123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/account - Success")
    void deleteAccount_success() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/account")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(authService).deleteAccount("u1");
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Success")
    void getCurrentUser_success() throws Exception {
        UserProfileDto profile = UserProfileDto.builder().userId("u1").fullName("Test User").build();
        Mockito.when(authService.getUserById("u1")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/deactivate - Success")
    void deactivateAccount_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/deactivate")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(authService).deactivateAccount("u1");
    }

    @Test
    @DisplayName("GET /api/v1/auth/search - Success")
    void searchUsers_success() throws Exception {
        UserProfileDto profile = UserProfileDto.builder().userId("u1").fullName("Test User").build();
        Mockito.when(authService.searchUsers("test")).thenReturn(java.util.List.of(profile));

        mockMvc.perform(get("/api/v1/auth/search")
                .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"));
    }
}
