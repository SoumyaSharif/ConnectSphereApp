package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndStatus(String userId, Payment.PaymentStatus status);

    Optional<Payment> findFirstByUserIdAndStatusOrderByPaidAtDesc(String userId, Payment.PaymentStatus status);
}
