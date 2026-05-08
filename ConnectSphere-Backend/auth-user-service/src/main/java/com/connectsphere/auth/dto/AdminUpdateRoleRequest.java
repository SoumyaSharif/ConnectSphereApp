package com.connectsphere.auth.dto;

import com.connectsphere.auth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateRoleRequest {
    @NotNull(message = "Role must not be null")
    private User.Role role;
}
