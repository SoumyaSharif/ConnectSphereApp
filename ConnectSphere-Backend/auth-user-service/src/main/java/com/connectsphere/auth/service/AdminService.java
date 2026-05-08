package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminDashboardStatsDto getDashboardStats();

    PagedResponse<AdminUserSummaryDto> getUsers(
            String query,
            User.Role role,
            User.AuthProvider provider,
            Boolean active,
            Boolean verified,
            Pageable pageable
    );

    AdminUserDetailDto getUserDetail(String userId);

    void updateUserRole(String adminUserId, String targetUserId, AdminUpdateRoleRequest request);

    void updateUserStatus(String adminUserId, String targetUserId, AdminUpdateStatusRequest request);

    void deleteUser(String adminUserId, String targetUserId);

    void approveVerification(String adminUserId, String targetUserId);

    void denyVerification(String adminUserId, String targetUserId);
}
