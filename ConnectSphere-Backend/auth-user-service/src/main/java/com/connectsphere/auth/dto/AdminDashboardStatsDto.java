package com.connectsphere.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin dashboard stats response.
 * User-tier stats come from the auth-user-service directly.
 * Social stats (posts, comments, likes, follows) are fetched from sibling services and may be null
 * if those services are temporarily unavailable.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardStatsDto {

    // --- User stats (always available) ---
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long adminCount;
    private long verifiedUsers;
    private long localUsers;
    private long googleUsers;
    private long newUsersLast7Days;
    private long newUsersLast30Days;

    // --- Social stats (from sibling services, nullable if unavailable) ---
    private Long totalPosts;
    private Long totalComments;
    private Long totalLikes;
    private Long totalFollows;
}
