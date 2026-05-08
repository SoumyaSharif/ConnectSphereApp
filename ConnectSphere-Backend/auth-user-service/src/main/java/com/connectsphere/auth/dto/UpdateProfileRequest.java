package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Email(message = "Invalid email format")
    private String email;
    private String username;
    private String fullName;
    private String bio;
    private String profilePicUrl;
}
