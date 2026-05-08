package com.connectsphere.auth.config;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminBootstrapRunner — runs once at startup.
 *
 * If the ADMIN_SEED_EMAIL environment variable is set, this runner will promote
 * the matching user to ADMIN role if they exist. If no admins exist at all and
 * the env var is not set, a warning is logged.
 *
 * This is the recommended way to create the first admin in a fresh deployment.
 * In production, remove or clear ADMIN_SEED_EMAIL after initial setup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.seed-email:}")
    private String adminSeedEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long adminCount = userRepository.countByRole(User.Role.ADMIN);

        if (adminSeedEmail != null && !adminSeedEmail.isBlank()) {
            userRepository.findByEmail(adminSeedEmail).ifPresentOrElse(user -> {
                if (user.getRole() != User.Role.ADMIN) {
                    user.setRole(User.Role.ADMIN);
                    userRepository.save(user);
                    log.info("AdminBootstrap: promoted [{}] to ADMIN role.", adminSeedEmail);
                } else {
                    log.info("AdminBootstrap: [{}] is already an ADMIN — no change.", adminSeedEmail);
                }
            }, () -> log.warn("AdminBootstrap: ADMIN_SEED_EMAIL is set but no user found with email: {}", adminSeedEmail));
        }

        // Re-check after any promotion
        long finalAdminCount = userRepository.countByRole(User.Role.ADMIN);
        if (finalAdminCount == 0) {
            log.warn("AdminBootstrap: WARNING — no ADMIN users exist in the database. " +
                     "Set ADMIN_SEED_EMAIL env var or run the SQL: " +
                     "UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';");
        } else {
            log.info("AdminBootstrap: {} ADMIN user(s) active in platform.", finalAdminCount);
        }
    }
}
