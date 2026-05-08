package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminResource MockMvc Tests")
class AdminResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/v1/auth/admin/dashboard - Success")
    void getDashboard() throws Exception {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();
        stats.setTotalUsers(100L);
        Mockito.when(adminService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/auth/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/auth/admin/users - Success")
    void listUsers() throws Exception {
        PagedResponse<AdminUserSummaryDto> page = new PagedResponse<>();
        page.setTotalElements(50L);
        Mockito.when(adminService.getUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/auth/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/auth/admin/users/{userId} - Success")
    void getUserDetail() throws Exception {
        AdminUserDetailDto detail = new AdminUserDetailDto();
        detail.setUserId("u1");
        Mockito.when(adminService.getUserDetail("u1")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/auth/admin/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/admin/users/{userId}/role - Success")
    void updateRole() throws Exception {
        AdminUpdateRoleRequest req = new AdminUpdateRoleRequest();
        req.setRole(User.Role.ADMIN);

        mockMvc.perform(patch("/api/v1/auth/admin/users/u1/role")
                .header("X-User-Id", "admin1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(adminService).updateUserRole(eq("admin1"), eq("u1"), any(AdminUpdateRoleRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/admin/users/{userId}/status - Success")
    void updateStatus() throws Exception {
        AdminUpdateStatusRequest req = new AdminUpdateStatusRequest();
        req.setActive(false);

        mockMvc.perform(patch("/api/v1/auth/admin/users/u1/status")
                .header("X-User-Id", "admin1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User suspended."));

        Mockito.verify(adminService).updateUserStatus(eq("admin1"), eq("u1"), any(AdminUpdateStatusRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/admin/users/{userId} - Success")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/admin/users/u1")
                .header("X-User-Id", "admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(adminService).deleteUser("admin1", "u1");
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/admin/users/{userId}/verify - Success")
    void approveVerification() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/admin/users/u1/verify")
                .header("X-User-Id", "admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Mockito.verify(adminService).approveVerification("admin1", "u1");
    }
}
