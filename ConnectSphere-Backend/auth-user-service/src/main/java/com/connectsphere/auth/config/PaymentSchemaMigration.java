package com.connectsphere.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration to extend the payments.status ENUM column to include
 * REFUNDED and REFUND_FAILED values that were added after initial schema creation.
 *
 * Hibernate's ddl-auto=update does NOT modify existing ENUM column definitions,
 * so this component runs the ALTER TABLE on every startup (idempotent — MySQL
 * treats a no-op ALTER as instant if the column is already correct).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migratePaymentStatusColumn() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE payments MODIFY COLUMN status " +
                "ENUM('CREATED','PAID','REFUNDED','FAILED','REFUND_FAILED') " +
                "NOT NULL DEFAULT 'CREATED'"
            );
            log.info("PaymentSchemaMigration: payments.status column is up-to-date.");
        } catch (Exception e) {
            // If the column already matches (or table doesn't exist yet), this is safe to ignore.
            log.debug("PaymentSchemaMigration skipped or already applied: {}", e.getMessage());
        }
    }
}
