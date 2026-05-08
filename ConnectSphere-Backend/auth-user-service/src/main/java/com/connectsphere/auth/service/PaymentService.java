package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.CreateOrderResponse;
import com.connectsphere.auth.dto.PaymentVerificationRequest;
import com.connectsphere.auth.dto.PaymentVerificationResponse;

public interface PaymentService {
    /**
     * Creates a Razorpay order for the blue-tick verification fee.
     *
     * @param userId the authenticated user's ID
     * @return order details needed by the Razorpay Checkout JS modal
     */
    CreateOrderResponse createOrder(String userId);

    /**
     * Verifies the Razorpay HMAC signature after the user completes checkout.
     * On success, marks the user as verified (blue tick).
     *
     * @param userId  the authenticated user's ID
     * @param request the three IDs returned by Razorpay JS
     * @return result of verification and updated isVerified flag
     */
    PaymentVerificationResponse verifyPayment(String userId, PaymentVerificationRequest request);

    /**
     * Removes the blue-tick verification status for the user.
     *
     * @param userId the authenticated user's ID
     */
    PaymentVerificationResponse cancelVerification(String userId);

    /**
     * Returns whether the user currently has a verified (paid) status.
     *
     * @param userId the authenticated user's ID
     * @return true if the user's isVerified flag is set
     */
    boolean getVerificationStatus(String userId);

    boolean getVerificationPendingStatus(String userId);

    /**
     * Initiates a refund for the blue-tick verification payment.
     *
     * @param userId the user's ID
     * @param reason reason for the refund
     * @return payment verification response with status and message
     */
    PaymentVerificationResponse refundVerification(String userId, String reason);
}
