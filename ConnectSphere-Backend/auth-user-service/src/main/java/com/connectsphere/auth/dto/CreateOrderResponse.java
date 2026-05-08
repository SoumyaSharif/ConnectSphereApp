package com.connectsphere.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to frontend after creating a Razorpay order.
 * Frontend uses these to open the Razorpay Checkout modal.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    private String orderId;
    private int amount;
    private String currency;
    private String keyId;
    private String description;
}
