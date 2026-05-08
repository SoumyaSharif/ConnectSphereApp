package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl Tests")
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AdminServiceImpl adminService;

    private User mockAdmin(String id) {
        return User.builder()
                .userId(id)
                .username("admin_" + id)
                .role(User.Role.ADMIN)
                .build();
    }

    private User mockUser(String id) {
        return User.builder()
                .userId(id)
                .username("user_" + id)
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("getDashboardStats: fetches stats gracefully")
    void getDashboardStats() {
        Mockito.when(userRepository.count()).thenReturn(100L);
        Mockito.when(userRepository.countByIsActive(true)).thenReturn(90L);
        Mockito.when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(2L);
        
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        
        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getActiveUsers()).isEqualTo(90L);
        assertThat(stats.getAdminCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getUsers: success")
    void getUsers() {
        Page<User> page = new PageImpl<>(List.of(mockUser("u1")));
        Mockito.when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResponse<AdminUserSummaryDto> resp = adminService.getUsers("query", null, null, null, null, Pageable.unpaged());

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUserDetail: success")
    void getUserDetail() {
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(mockUser("u1")));
        AdminUserDetailDto dto = adminService.getUserDetail("u1");
        assertThat(dto.getUserId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("updateUserRole: prevents self change")
    void updateUserRole_selfChange() {
        AdminUpdateRoleRequest req = new AdminUpdateRoleRequest();
        req.setRole(User.Role.USER);
        assertThatThrownBy(() -> adminService.updateUserRole("admin1", "admin1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot change their own role");
    }

    @Test
    @DisplayName("updateUserRole: prevents demoting last admin")
    void updateUserRole_lastAdmin() {
        AdminUpdateRoleRequest req = new AdminUpdateRoleRequest();
        req.setRole(User.Role.USER);
        User target = mockAdmin("admin2");
        
        Mockito.when(userRepository.findById("admin2")).thenReturn(Optional.of(target));
        Mockito.when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminService.updateUserRole("admin1", "admin2", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot demote the last remaining admin");
    }

    @Test
    @DisplayName("updateUserRole: success")
    void updateUserRole_success() {
        AdminUpdateRoleRequest req = new AdminUpdateRoleRequest();
        req.setRole(User.Role.ADMIN);
        User target = mockUser("u1");
        
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        adminService.updateUserRole("admin1", "u1", req);
        
        assertThat(target.getRole()).isEqualTo(User.Role.ADMIN);
        Mockito.verify(userRepository).save(target);
    }

    @Test
    @DisplayName("updateUserStatus: prevents self suspend")
    void updateUserStatus_selfSuspend() {
        AdminUpdateStatusRequest req = new AdminUpdateStatusRequest();
        req.setActive(false);
        assertThatThrownBy(() -> adminService.updateUserStatus("admin1", "admin1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot suspend their own account");
    }

    @Test
    @DisplayName("updateUserStatus: success")
    void updateUserStatus_success() {
        AdminUpdateStatusRequest req = new AdminUpdateStatusRequest();
        req.setActive(false);
        User target = mockUser("u1");
        target.setActive(true);
        
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        adminService.updateUserStatus("admin1", "u1", req);
        
        assertThat(target.isActive()).isFalse();
        Mockito.verify(userRepository).save(target);
    }

    @Test
    @DisplayName("deleteUser: prevents self delete")
    void deleteUser_selfDelete() {
        assertThatThrownBy(() -> adminService.deleteUser("admin1", "admin1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot delete their own account");
    }

    @Test
    @DisplayName("deleteUser: prevents deleting last admin")
    void deleteUser_lastAdmin() {
        User target = mockAdmin("admin2");
        Mockito.when(userRepository.findById("admin2")).thenReturn(Optional.of(target));
        Mockito.when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminService.deleteUser("admin1", "admin2"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the last remaining admin");
    }

    @Test
    @DisplayName("deleteUser: success")
    void deleteUser_success() {
        User target = mockUser("u1");
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        adminService.deleteUser("admin1", "u1");
        
        Mockito.verify(userRepository).deleteById("u1");
    }

    @Test
    @DisplayName("approveVerification: success")
    void approveVerification_success() {
        User target = mockUser("u1");
        target.setVerificationPending(true);
        target.setVerified(false);

        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        adminService.approveVerification("admin1", "u1");

        assertThat(target.isVerified()).isTrue();
        assertThat(target.isVerificationPending()).isFalse();
        Mockito.verify(userRepository).save(target);
    }

    @Test
    @DisplayName("approveVerification: not pending")
    void approveVerification_notPending() {
        User target = mockUser("u1");
        target.setVerificationPending(false);

        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.approveVerification("admin1", "u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no pending verification request");
    }

    @Test
    @DisplayName("approveVerification: already verified")
    void approveVerification_alreadyVerified() {
        User target = mockUser("u1");
        target.setVerificationPending(true);
        target.setVerified(true);

        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.approveVerification("admin1", "u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    @DisplayName("getDashboardStats: fetches external stats gracefully")
    void getDashboardStats_external() {
        Mockito.when(restTemplate.getForObject(anyString(), eq(java.util.Map.class)))
                .thenReturn(java.util.Map.of("totalPosts", 50, "totalComments", "invalid")); // Valid int -> Long, Invalid string -> null

        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        
        assertThat(stats.getTotalPosts()).isEqualTo(50L);
        assertThat(stats.getTotalComments()).isNull();
    }
}
