package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminResource — all admin-only CRUD operations.
 *
 * Security: guarded at two layers:
 *   1. SecurityConfig matchers: /api/v1/auth/admin/** → hasRole("ADMIN")
 *   2. Class-level @PreAuthorize: double enforcement via method security
 *
 * Identity: the acting admin's userId is read from the X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/v1/auth/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only platform management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminResource {

    private final AdminService adminService;

    /**
     * GET /api/v1/auth/admin/dashboard
     * Returns platform-wide stats: user counts + social aggregate counts from sibling services.
     */
    @Operation(summary = "Get admin dashboard stats")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStatsDto> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    /**
     * GET /api/v1/auth/admin/users
     * Paginated, filterable user listing.
     *
     * @param query    free-text search on username/email/fullName
     * @param role     filter by User.Role (USER, ADMIN, GUEST)
     * @param provider filter by AuthProvider (LOCAL, GOOGLE)
     * @param active   filter by active status
     * @param verified filter by verified status
     * @param page     0-indexed page number (default 0)
     * @param size     page size (default 20, max 100)
     * @param sort     field,direction e.g. "createdAt,desc" (default createdAt,desc)
     */
    @Operation(summary = "List users with pagination and filters")
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<AdminUserSummaryDto>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) User.AuthProvider provider,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // Cap page size to prevent abuse
        size = Math.min(size, 100);

        Sort springSort;
        try {
            String[] parts = sort.split(",");
            String field = parts[0];
            Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            springSort = Sort.by(dir, field);
        } catch (Exception e) {
            springSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, springSort);
        return ResponseEntity.ok(adminService.getUsers(query, role, provider, active, verified, pageable));
    }

    /**
     * GET /api/v1/auth/admin/users/{userId}
     * Full detail for a single user (used in the admin side drawer).
     */
    @Operation(summary = "Get full admin detail for a single user")
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    /**
     * PATCH /api/v1/auth/admin/users/{userId}/role
     * Update a user's role. An admin cannot change their own role.
     * Cannot demote the last remaining admin.
     */
    @Operation(summary = "Update user role")
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @RequestHeader("X-User-Id") String adminUserId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUpdateRoleRequest request) {
        adminService.updateUserRole(adminUserId, userId, request);
        return ResponseEntity.ok(Map.of("message", "Role updated to " + request.getRole()));
    }

    /**
     * PATCH /api/v1/auth/admin/users/{userId}/status
     * Suspend (active=false) or reactivate (active=true) a user.
     * An admin cannot suspend themselves.
     */
    @Operation(summary = "Suspend or reactivate a user")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @RequestHeader("X-User-Id") String adminUserId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUpdateStatusRequest request) {
        adminService.updateUserStatus(adminUserId, userId, request);
        String msg = Boolean.TRUE.equals(request.getActive()) ? "User reactivated." : "User suspended.";
        return ResponseEntity.ok(Map.of("message", msg));
    }

    /**
     * DELETE /api/v1/auth/admin/users/{userId}
     * Permanently delete a user. Cannot delete self or last admin.
     */
    @Operation(summary = "Permanently delete a user")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @RequestHeader("X-User-Id") String adminUserId,
            @PathVariable String userId) {
        adminService.deleteUser(adminUserId, userId);
        return ResponseEntity.ok(Map.of("message", "User permanently deleted."));
    }

    /**
     * PATCH /api/v1/auth/admin/users/{userId}/verify
     * Approve a user's pending verification request → grants blue tick (isVerified=true).
     * Requires verificationPending=true on the target user (set after successful payment).
     */
    @Operation(summary = "Approve user verification (grant blue tick)")
    @PatchMapping("/users/{userId}/verify")
    public ResponseEntity<Map<String, String>> approveVerification(
            @RequestHeader("X-User-Id") String adminUserId,
            @PathVariable String userId) {
        adminService.approveVerification(adminUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Verification approved. Blue tick granted."));
    }

    @Operation(summary = "Deny user verification (refund payment)")
    @PatchMapping("/users/{userId}/deny-verification")
    public ResponseEntity<Map<String, String>> denyVerification(
            @RequestHeader("X-User-Id") String adminUserId,
            @PathVariable String userId) {
        adminService.denyVerification(adminUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Verification denied. Refund initiated."));
    }
}
