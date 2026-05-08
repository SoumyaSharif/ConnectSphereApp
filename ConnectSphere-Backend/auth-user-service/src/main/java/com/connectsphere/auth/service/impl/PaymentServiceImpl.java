package com.connectsphere.auth.service.impl;

import com.connectsphere.auth.dto.CreateOrderResponse;
import com.connectsphere.auth.dto.PaymentVerificationRequest;
import com.connectsphere.auth.dto.PaymentVerificationResponse;
import com.connectsphere.auth.entity.Payment;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.PaymentRepository;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.PaymentService;
import com.connectsphere.auth.email.EmailService;
import com.razorpay.Order;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Razorpay payment service implementation.
 *
 * Flow:
 *  1. createOrder()  — calls Razorpay API, persists a CREATED payment row, returns order details to frontend.
 *  2. verifyPayment() — validates HMAC-SHA256 signature, marks payment as PAID, sets user.isVerified = true.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository     userRepository;
    private final PaymentRepository  paymentRepository;
    private final EmailService       emailService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.amount-paise}")
    private int amountPaise;

    @Value("${app.razorpay.currency}")
    private String currency;

    // ------------------------------------------------------------------ //
    //  Create Order                                                        //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public CreateOrderResponse createOrder(String userId) {
        User user = resolveUser(userId);
        userId = user.getUserId(); // Ensure we have the actual ID for subsequent logic

        if (user.isVerified()) {
            throw new BadRequestException("User is already verified.");
        }

        if (user.isVerificationPending()) {
            throw new BadRequestException("Verification payment is already completed and waiting for admin approval.");
        }

        try {
            log.info("Preparing Razorpay order: user={}, amount={}, currency={}", userId, amountPaise, currency);
            RazorpayClient client = buildClient();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",   amountPaise);
            orderRequest.put("currency", currency);
            
            // Safer receipt ID generation
            String receiptId = "cs_verify_" + (userId.length() > 8 ? userId.substring(0, 8) : userId);
            orderRequest.put("receipt",  receiptId);
            orderRequest.put("payment_capture", 1);

            log.info("Sending order request to Razorpay: {}", orderRequest);
            Order razorpayOrder = client.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");
            log.info("Razorpay order created successfully: {}", orderId);

            // Persist the attempt
            Payment payment = Payment.builder()
                    .userId(userId)
                    .razorpayOrderId(orderId)
                    .amount(amountPaise)
                    .currency(currency)
                    .status(Payment.PaymentStatus.CREATED)
                    .build();
            paymentRepository.save(payment);

            log.info("Razorpay order created: {} for user: {}", orderId, userId);

            return CreateOrderResponse.builder()
                    .orderId(orderId)
                    .amount(amountPaise)
                    .currency(currency)
                    .keyId(razorpayKeyId)
                    .description("ConnectSphere Blue Tick Verification – $2.99")
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay API error for user {}: {}", userId, e.getMessage());
            throw new BadRequestException("Payment service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during order creation for user {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("An unexpected error occurred: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Verify Payment                                                      //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public PaymentVerificationResponse verifyPayment(String userId, PaymentVerificationRequest request) {
        User user = resolveUser(userId);
        userId = user.getUserId();

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new BadRequestException("Order not found: " + request.getRazorpayOrderId()));

        if (!payment.getUserId().equals(userId)) {
            throw new BadRequestException("Payment does not belong to this user.");
        }

        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            return PaymentVerificationResponse.builder()
                    .success(true)
                    .message("Verification payment already received and waiting for admin approval.")
                    .isVerified(user.isVerified())
                    .verificationPending(user.isVerificationPending())
                    .build();
        }

        boolean signatureValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!signatureValid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Invalid Razorpay signature for order {} / user {}", request.getRazorpayOrderId(), userId);
            return PaymentVerificationResponse.builder()
                    .success(false)
                    .message("Payment verification failed. Please contact support.")
                    .isVerified(false)
                    .verificationPending(false)
                    .build();
        }

        // Mark payment as PAID
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        user.setVerified(false);
        user.setVerificationPending(true);
        userRepository.save(user);

        log.info("Verification payment recorded for user: {} via order: {}", user.getUserId(), request.getRazorpayOrderId());
        
        emailService.sendPaymentReceiptEmail(user.getEmail(), user.getFullName(), payment.getAmount(), payment.getCurrency(), payment.getRazorpayOrderId());

        return PaymentVerificationResponse.builder()
                .success(true)
                .message("Payment successful. Please wait for admin approval.")
                .isVerified(false)
                .verificationPending(true)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Status                                                              //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public PaymentVerificationResponse cancelVerification(String userId) {
        User user = resolveUser(userId);
        return refundVerification(user.getUserId(), "Blue tick removed by user");
    }

    @Override
    @Transactional
    public PaymentVerificationResponse refundVerification(String userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.isVerified() && !user.isVerificationPending()) {
            return PaymentVerificationResponse.builder()
                    .success(true)
                    .message("Verification is already inactive.")
                    .isVerified(false)
                    .verificationPending(false)
                    .build();
        }

        Payment payment = paymentRepository.findFirstByUserIdAndStatusOrderByPaidAtDesc(
                        userId,
                        Payment.PaymentStatus.PAID
                )
                .orElseThrow(() -> new BadRequestException("No paid verification purchase found to refund."));

        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            throw new BadRequestException("Refund cannot be processed because the payment reference is missing.");
        }

        try {
            RazorpayClient client = buildClient();
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", payment.getAmount());
            refundRequest.put("speed", "normal");
            refundRequest.put("notes", new JSONObject()
                    .put("reason", reason)
                    .put("userId", userId));

            Refund refund = client.payments.refund(payment.getRazorpayPaymentId(), refundRequest);

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refund.get("id"));
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            user.setVerified(false);
            user.setVerificationPending(false);
            userRepository.save(user);

            log.info("Refunded verification payment {} for user {}. Reason: {}", payment.getRazorpayPaymentId(), userId, reason);

            return PaymentVerificationResponse.builder()
                    .success(true)
                    .message("Verification request removed and refund started successfully.")
                    .isVerified(false)
                    .verificationPending(false)
                    .build();
        } catch (RazorpayException e) {
            payment.setStatus(Payment.PaymentStatus.REFUND_FAILED);
            paymentRepository.save(payment);
            log.error("Refund failed for user {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public boolean getVerificationStatus(String userId) {
        User user = resolveUser(userId);
        return user.isVerified();
    }

    @Override
    public boolean getVerificationPendingStatus(String userId) {
        User user = resolveUser(userId);
        return user.isVerificationPending();
    }

    private User resolveUser(String userId) {
        String normalizedUserId = normalizeUserId(userId);
        if (normalizedUserId != null) {
            return userRepository.findById(normalizedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + normalizedUserId));
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Authentication missing in SecurityContext for PaymentService");
            throw new BadRequestException("User not authenticated.");
        }

        Object principal = auth.getPrincipal();
        log.info("Resolving user from principal: {}", principal);
        
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }

        String normalized = userId.split(",")[0].trim();
        if (normalized.isBlank() || "undefined".equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }

    private RazorpayClient buildClient() throws RazorpayException {
        return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    // ------------------------------------------------------------------ //
    //  HMAC-SHA256 Signature Verification                                  //
    // ------------------------------------------------------------------ //

    private boolean verifySignature(String orderId, String paymentId, String actualSignature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);
            return expectedSignature.equals(actualSignature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
