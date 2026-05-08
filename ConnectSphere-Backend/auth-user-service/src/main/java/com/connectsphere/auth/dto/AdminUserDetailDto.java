package com.connectsphere.auth.dto;

import com.connectsphere.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full admin-safe user detail — shown in the admin side drawer.
 * Includes all profile fields and timestamps. Never exposes password hash or reset tokens.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDetailDto {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profilePicUrl;
    private User.Role role;
    private User.AuthProvider provider;
    @JsonProperty("isActive")
    private boolean isActive;
    @JsonProperty("isVerified")
    private boolean isVerified;
    @JsonProperty("verificationPending")
    private boolean verificationPending;
    private LocalDateTime verificationDeniedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
