package com.connectsphere.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned after server-side payment signature verification.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationResponse {
    private boolean success;
    private String message;
    @JsonProperty("isVerified")
    private boolean isVerified;
    @JsonProperty("verificationPending")
    private boolean verificationPending;
}
