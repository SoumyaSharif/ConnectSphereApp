package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.PaymentVerificationRequest;
import com.connectsphere.auth.dto.PaymentVerificationResponse;
import com.connectsphere.auth.email.EmailService;
import com.connectsphere.auth.entity.Payment;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.repository.PaymentRepository;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.impl.PaymentServiceImpl;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Tests")
class PaymentServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EmailService emailService;

    @InjectMocks private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "key_test_123");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "secret_123");
        ReflectionTestUtils.setField(paymentService, "amountPaise", 50000);
        ReflectionTestUtils.setField(paymentService, "currency", "INR");
    }

    private User mockUser(String id, boolean verified, boolean pending) {
        return User.builder()
                .userId(id)
                .email("test@example.com")
                .fullName("Test User")
                .isVerified(verified)
                .verificationPending(pending)
                .build();
    }

    @Test
    @DisplayName("createOrder: user already verified throws exception")
    void createOrder_alreadyVerified() {
        User u = mockUser("u1", true, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> paymentService.createOrder("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    @DisplayName("createOrder: user verification pending throws exception")
    void createOrder_pending() {
        User u = mockUser("u1", false, true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> paymentService.createOrder("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("waiting for admin approval");
    }

    @Test
    @DisplayName("createOrder: Razorpay API error")
    void createOrder_apiError() throws Exception {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        OrderClient mockOrders = mock(OrderClient.class);
        when(mockOrders.create(any())).thenThrow(new RazorpayException("Simulated API Error"));

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    ReflectionTestUtils.setField(mock, "orders", mockOrders);
                })) {
            
            assertThatThrownBy(() -> paymentService.createOrder("u1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Payment service error");
        }
    }

    @Test
    @DisplayName("createOrder: unexpected exception")
    void createOrder_unexpected() throws Exception {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        OrderClient mockOrders = mock(OrderClient.class);
        when(mockOrders.create(any())).thenThrow(new RuntimeException("Unexpected DB error"));

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    ReflectionTestUtils.setField(mock, "orders", mockOrders);
                })) {
            
            assertThatThrownBy(() -> paymentService.createOrder("u1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("unexpected error");
        }
    }

    @Test
    @DisplayName("verifyPayment: order not found")
    void verifyPayment_orderNotFound() {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(paymentRepository.findByRazorpayOrderId("ord_123")).thenReturn(Optional.empty());

        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", "sig_123");
        
        assertThatThrownBy(() -> paymentService.verifyPayment("u1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("verifyPayment: wrong user")
    void verifyPayment_wrongUser() {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u2").build();
        when(paymentRepository.findByRazorpayOrderId("ord_123")).thenReturn(Optional.of(payment));

        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", "sig_123");
        
        assertThatThrownBy(() -> paymentService.verifyPayment("u1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    @DisplayName("verifyPayment: already paid")
    void verifyPayment_alreadyPaid() {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u1").status(Payment.PaymentStatus.PAID).build();
        when(paymentRepository.findByRazorpayOrderId("ord_123")).thenReturn(Optional.of(payment));

        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", "sig_123");
        PaymentVerificationResponse resp = paymentService.verifyPayment("u1", req);
        
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).contains("already received");
    }

    @Test
    @DisplayName("verifyPayment: invalid signature")
    void verifyPayment_invalidSignature() {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u1").status(Payment.PaymentStatus.CREATED).build();
        when(paymentRepository.findByRazorpayOrderId("ord_123")).thenReturn(Optional.of(payment));

        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", "bad_sig");
        PaymentVerificationResponse resp = paymentService.verifyPayment("u1", req);
        
        assertThat(resp.isSuccess()).isFalse();
        verify(paymentRepository).save(payment);
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("cancelVerification: no pending/verified")
    void cancelVerification_inactive() {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        PaymentVerificationResponse resp = paymentService.cancelVerification("u1");
        
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).contains("already inactive");
    }

    @Test
    @DisplayName("cancelVerification: no paid payment found")
    void cancelVerification_noPayment() {
        User u = mockUser("u1", true, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(paymentRepository.findFirstByUserIdAndStatusOrderByPaidAtDesc("u1", Payment.PaymentStatus.PAID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelVerification("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No paid verification");
    }

    @Test
    @DisplayName("cancelVerification: missing razorpay payment id")
    void cancelVerification_missingPaymentId() {
        User u = mockUser("u1", true, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u1").status(Payment.PaymentStatus.PAID).build();
        when(paymentRepository.findFirstByUserIdAndStatusOrderByPaidAtDesc("u1", Payment.PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelVerification("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("getVerificationStatus returns true")
    void getVerificationStatus() {
        User u = mockUser("u1", true, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        assertThat(paymentService.getVerificationStatus("u1")).isTrue();
    }

    @Test
    @DisplayName("getVerificationPendingStatus returns true")
    void getVerificationPendingStatus() {
        User u = mockUser("u1", false, true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        assertThat(paymentService.getVerificationPendingStatus("u1")).isTrue();
    }

    @Test
    @DisplayName("verifyPayment: success")
    void verifyPayment_success() throws Exception {
        User u = mockUser("u1", false, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u1").status(Payment.PaymentStatus.CREATED).amount(50000).currency("INR").build();
        when(paymentRepository.findByRazorpayOrderId("ord_123")).thenReturn(Optional.of(payment));

        String payload = "ord_123|pay_123";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("secret_123".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String validSig = java.util.HexFormat.of().formatHex(hash);

        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", validSig);
        PaymentVerificationResponse resp = paymentService.verifyPayment("u1", req);
        
        assertThat(resp.isSuccess()).isTrue();
        verify(paymentRepository).save(payment);
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        verify(userRepository).save(u);
        assertThat(u.isVerificationPending()).isTrue();
        verify(emailService).sendPaymentReceiptEmail(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("cancelVerification: success")
    void cancelVerification_success() throws Exception {
        User u = mockUser("u1", true, false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        
        Payment payment = Payment.builder().userId("u1").status(Payment.PaymentStatus.PAID).razorpayPaymentId("pay_123").amount(50000).build();
        when(paymentRepository.findFirstByUserIdAndStatusOrderByPaidAtDesc("u1", Payment.PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));

        com.razorpay.PaymentClient mockPayments = mock(com.razorpay.PaymentClient.class);
        com.razorpay.Refund mockRefundObj = new com.razorpay.Refund(new org.json.JSONObject("{\"id\":\"rfnd_123\"}"));
        when(mockPayments.refund(any(), any())).thenReturn(mockRefundObj);

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    ReflectionTestUtils.setField(mock, "payments", mockPayments);
                })) {
            
            PaymentVerificationResponse resp = paymentService.cancelVerification("u1");
            assertThat(resp.isSuccess()).isTrue();
            verify(paymentRepository).save(payment);
            assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }
    }

    @Test
    @DisplayName("resolveUser: fallback to SecurityContext")
    void resolveUser_fallback() {
        org.springframework.security.core.userdetails.UserDetails userDetails = 
            org.springframework.security.core.userdetails.User.withUsername("test@example.com").password("pwd").authorities("USER").build();
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        User u = mockUser("u1", false, false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(u));

        assertThat(paymentService.getVerificationStatus("undefined")).isFalse();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
