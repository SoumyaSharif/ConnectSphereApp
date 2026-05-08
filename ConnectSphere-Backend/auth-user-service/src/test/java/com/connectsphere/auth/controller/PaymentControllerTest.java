package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController MockMvc Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/v1/payments/create-order - Success")
    void createOrder() throws Exception {
        CreateOrderResponse mockResp = CreateOrderResponse.builder()
                .orderId("order123")
                .currency("INR")
                .amount(50000)
                .build();
        Mockito.when(paymentService.createOrder("u1")).thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/payments/create-order")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order123"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/verify - Success")
    void verifyPayment() throws Exception {
        PaymentVerificationRequest req = new PaymentVerificationRequest("ord_123", "pay_123", "sig_123");
        PaymentVerificationResponse mockResp = PaymentVerificationResponse.builder()
                .success(true)
                .message("Success")
                .build();

        Mockito.when(paymentService.verifyPayment(eq("u1"), any(PaymentVerificationRequest.class))).thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/payments/verify")
                .header("X-User-Id", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/payments/my-status - Success")
    void getMyStatus() throws Exception {
        Mockito.when(paymentService.getVerificationStatus("u1")).thenReturn(true);
        Mockito.when(paymentService.getVerificationPendingStatus("u1")).thenReturn(false);

        mockMvc.perform(get("/api/v1/payments/my-status")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.verificationPending").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/cancel - Success")
    void cancelVerification() throws Exception {
        PaymentVerificationResponse mockResp = PaymentVerificationResponse.builder()
                .success(true)
                .message("Cancelled")
                .build();
        Mockito.when(paymentService.cancelVerification("u1")).thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/payments/cancel")
                .header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
