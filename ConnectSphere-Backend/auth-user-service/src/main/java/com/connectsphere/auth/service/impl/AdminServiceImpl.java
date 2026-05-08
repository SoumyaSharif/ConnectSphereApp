package com.connectsphere.auth.service.impl;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.service.AdminService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminServiceImpl — all platform-admin operations.
 *
 * Safety rules enforced here (in addition to @PreAuthorize at controller level):
 *  - Admin cannot change their own role
 *  - Admin cannot suspend themselves
 *  - Admin cannot delete themselves
 *  - Cannot delete the last remaining ADMIN
 *  - Cannot demote the last remaining ADMIN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final com.connectsphere.auth.service.PaymentService paymentService;
    private final com.connectsphere.auth.repository.PaymentRepository paymentRepository;
    private final com.connectsphere.auth.email.EmailService emailService;

    @Value("${app.services.post-service:http://post-service}")
    private String postServiceUrl;

    @Value("${app.services.comment-service:http://comment-service}")
    private String commentServiceUrl;

    @Value("${app.services.like-service:http://like-service}")
    private String likeServiceUrl;

    @Value("${app.services.follow-service:http://follow-service}")
    private String followServiceUrl;

    // ---- Dashboard Stats ----

    @Override
    public AdminDashboardStatsDto getDashboardStats() {
        long totalUsers      = userRepository.count();
        long activeUsers     = userRepository.countByIsActive(true);
        long suspendedUsers  = userRepository.countByIsActive(false);
        long adminCount      = userRepository.countByRole(User.Role.ADMIN);
        long verifiedUsers   = userRepository.countByIsVerified(true);
        long localUsers      = userRepository.countByProvider(User.AuthProvider.LOCAL);
        long googleUsers     = userRepository.countByProvider(User.AuthProvider.GOOGLE);
        long newUsersLast7d  = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));
        long newUsersLast30d = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));

        // Fetch cross-service social stats — fail gracefully per service
        Long totalPosts    = fetchLong(postServiceUrl    + "/api/v1/posts/admin/stats",    "totalPosts");
        Long totalComments = fetchLong(commentServiceUrl + "/api/v1/comments/admin/stats", "totalComments");
        Long totalLikes    = fetchLong(likeServiceUrl    + "/api/v1/likes/admin/stats",    "totalLikes");
        Long totalFollows  = fetchLong(followServiceUrl  + "/api/v1/follows/admin/stats",  "totalFollows");

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .adminCount(adminCount)
                .verifiedUsers(verifiedUsers)
                .localUsers(localUsers)
                .googleUsers(googleUsers)
                .newUsersLast7Days(newUsersLast7d)
                .newUsersLast30Days(newUsersLast30d)
                .totalPosts(totalPosts)
                .totalComments(totalComments)
                .totalLikes(totalLikes)
                .totalFollows(totalFollows)
                .build();
    }

    // ---- User Listing ----

    @Override
    public PagedResponse<AdminUserSummaryDto> getUsers(
            String query, User.Role role, User.AuthProvider provider,
            Boolean active, Boolean verified, Pageable pageable) {

        Specification<User> spec = buildSpec(query, role, provider, active, verified);
        Page<User> page = userRepository.findAll(spec, pageable);

        List<AdminUserSummaryDto> content = page.getContent()
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());

        return PagedResponse.<AdminUserSummaryDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ---- User Detail ----

    @Override
    public AdminUserDetailDto getUserDetail(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToDetail(user);
    }

    // ---- Role Update ----

    @Override
    @Transactional
    public void updateUserRole(String adminUserId, String targetUserId, AdminUpdateRoleRequest request) {
        if (adminUserId.equals(targetUserId)) {
            throw new BadRequestException("Admins cannot change their own role.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        // Prevent demoting the last ADMIN
        if (target.getRole() == User.Role.ADMIN && request.getRole() != User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot demote the last remaining admin.");
            }
        }

        User.Role previousRole = target.getRole();
        target.setRole(request.getRole());
        userRepository.save(target);
        log.info("Admin [{}] changed role of user [{}] from {} to {}", adminUserId, targetUserId, previousRole, request.getRole());
    }

    // ---- Status Update ----

    @Override
    @Transactional
    public void updateUserStatus(String adminUserId, String targetUserId, AdminUpdateStatusRequest request) {
        if (adminUserId.equals(targetUserId) && Boolean.FALSE.equals(request.getActive())) {
            throw new BadRequestException("Admins cannot suspend their own account.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        target.setActive(request.getActive());
        userRepository.save(target);
        log.info("Admin [{}] set active={} for user [{}]", adminUserId, request.getActive(), targetUserId);
    }

    // ---- Delete User ----

    @Override
    @Transactional
    public void deleteUser(String adminUserId, String targetUserId) {
        if (adminUserId.equals(targetUserId)) {
            throw new BadRequestException("Admins cannot delete their own account via the admin panel.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        // Protect last admin from deletion
        if (target.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot delete the last remaining admin.");
            }
        }

        userRepository.deleteById(targetUserId);
        log.info("Admin [{}] permanently deleted user [{}]", adminUserId, targetUserId);
    }

    // ---- Approve Verification ----

    @Override
    @Transactional
    public void approveVerification(String adminUserId, String targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (!target.isVerificationPending()) {
            throw new BadRequestException("User has no pending verification request.");
        }
        if (target.isVerified()) {
            throw new BadRequestException("User is already verified.");
        }

        target.setVerified(true);
        target.setVerificationPending(false);
        userRepository.save(target);

        sendSystemNotification(
                targetUserId,
                "Your account has been verified! You now have the blue tick.",
                "/profile/" + targetUserId
        );

        log.info("Admin [{}] approved verification (blue tick) for user [{}]", adminUserId, targetUserId);
    }

    @Override
    @Transactional
    public void denyVerification(String adminUserId, String targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (!target.isVerificationPending()) {
            throw new BadRequestException("User has no pending verification request.");
        }

        // 1. Fetch the payment details before refunding
        com.connectsphere.auth.entity.Payment payment = paymentRepository.findFirstByUserIdAndStatusOrderByPaidAtDesc(
                        targetUserId,
                        com.connectsphere.auth.entity.Payment.PaymentStatus.PAID
                )
                .orElseThrow(() -> new BadRequestException("No paid verification purchase found to refund."));

        String amountStr = String.format("%.2f", payment.getAmount() / 100.0);
        String currency = payment.getCurrency();
        String orderId = payment.getRazorpayOrderId();

        // 2. Process refund
        paymentService.refundVerification(targetUserId, "Verification denied by Admin");

        // 3. Set 7-day cooldown on the user
        User freshTarget = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));
        freshTarget.setVerificationDeniedUntil(LocalDateTime.now().plusDays(7));
        userRepository.save(freshTarget);

        // 4. Send email notification
        emailService.sendRefundEmail(target.getEmail(), target.getFullName(), amountStr, currency, orderId);

        // 5. Send in-app notification
        sendSystemNotification(
                targetUserId,
                "Your verification request was denied. A refund has been initiated.",
                "/settings"
        );

        log.info("Admin [{}] denied verification and initiated refund for user [{}]", adminUserId, targetUserId);
    }

    private void sendSystemNotification(String recipientId, String message, String deepLinkUrl) {
        try {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("recipientId", recipientId);
            request.put("type", "SYSTEM");
            request.put("message", message);
            request.put("targetId", recipientId);
            request.put("targetType", "USER");
            request.put("deepLinkUrl", deepLinkUrl);

            restTemplate.postForObject(
                    "http://NOTIFICATION-SERVICE/api/v1/notifications/internal",
                    request,
                    Object.class
            );
        } catch (Exception e) {
            log.error("Failed to send system notification to user {}: {}", recipientId, e.getMessage());
        }
    }

    // ---- Private Helpers ----

    private Specification<User> buildSpec(
            String query, User.Role role, User.AuthProvider provider, Boolean active, Boolean verified) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")),    pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)
                ));
            }
            if (role     != null) predicates.add(cb.equal(root.get("role"),     role));
            if (provider != null) predicates.add(cb.equal(root.get("provider"), provider));
            if (active   != null) predicates.add(cb.equal(root.get("isActive"), active));
            if (verified != null) predicates.add(cb.equal(root.get("isVerified"), verified));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminUserSummaryDto mapToSummary(User u) {
        return AdminUserSummaryDto.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .profilePicUrl(u.getProfilePicUrl())
                .role(u.getRole())
                .provider(u.getProvider())
                .isActive(u.isActive())
                .isVerified(u.isVerified())
                .verificationPending(u.isVerificationPending())
                .verificationDeniedUntil(u.getVerificationDeniedUntil())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private AdminUserDetailDto mapToDetail(User u) {
        return AdminUserDetailDto.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .bio(u.getBio())
                .profilePicUrl(u.getProfilePicUrl())
                .role(u.getRole())
                .provider(u.getProvider())
                .isActive(u.isActive())
                .isVerified(u.isVerified())
                .verificationPending(u.isVerificationPending())
                .verificationDeniedUntil(u.getVerificationDeniedUntil())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    /**
     * Fetches a single Long stat from a sibling service count endpoint.
     * Returns null if the service is unavailable (fail-open, no fake data).
     */
    @SuppressWarnings("unchecked")
    private Long fetchLong(String url, String key) {
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey(key)) {
                Object val = response.get(key);
                if (val instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch stat from {}: {}", url, e.getMessage());
        }
        return null;
    }
}
