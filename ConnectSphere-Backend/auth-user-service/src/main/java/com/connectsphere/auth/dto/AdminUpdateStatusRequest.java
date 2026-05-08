package com.connectsphere.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateStatusRequest {
    @NotNull(message = "Active flag must not be null")
    @JsonProperty("active")
    private Boolean active;
}
