package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.CreateOrderResponse;
import com.connectsphere.auth.dto.PaymentVerificationRequest;
import com.connectsphere.auth.dto.PaymentVerificationResponse;
import com.connectsphere.auth.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for blue-tick verification payments via Razorpay.
 *
 * Endpoints (all require a valid JWT):
 *   POST /api/v1/payments/create-order  — Create a Razorpay order
 *   POST /api/v1/payments/verify        — Verify payment signature + grant blue tick
 *   GET  /api/v1/payments/my-status     — Check own verification status
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Blue-tick verification payment endpoints (Razorpay)")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1: Creates a Razorpay order.
     * Frontend must call this first, then open the Razorpay Checkout modal with the returned orderId.
     */
    @Operation(summary = "Create Razorpay order for blue-tick verification",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/create-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(paymentService.createOrder(userId));
    }

    /**
     * Step 2: Verifies the payment signature returned by Razorpay Checkout JS.
     * On success, sets user.isVerified = true (blue tick granted).
     */
    @Operation(summary = "Verify Razorpay payment and grant blue tick",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody PaymentVerificationRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(userId, request));
    }

    /**
     * Returns whether the current user has a verified (blue tick) account.
     */
    @Operation(summary = "Get own verification status",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> getMyStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        boolean verified = paymentService.getVerificationStatus(userId);
        boolean pending = paymentService.getVerificationPendingStatus(userId);
        return ResponseEntity.ok(Map.of("isVerified", verified, "verificationPending", pending));
    }

    /**
     * Step 3 (Optional): Opt-out of blue-tick verification.
     */
    @Operation(summary = "Opt-out of blue-tick verification",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentVerificationResponse> cancelVerification(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(paymentService.cancelVerification(userId));
    }
}
