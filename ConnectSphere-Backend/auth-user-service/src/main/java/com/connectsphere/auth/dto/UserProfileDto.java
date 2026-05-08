package com.connectsphere.auth.dto;

import com.connectsphere.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
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
    private long followersCount;
    private long followingCount;
}
