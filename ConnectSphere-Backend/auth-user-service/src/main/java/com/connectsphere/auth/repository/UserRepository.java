package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.fullName LIKE %:query%")
    List<User> searchByUsernameOrFullName(@Param("query") String query);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    void deleteByUserId(String userId);

    // --- Admin count queries ---
    long countByRole(User.Role role);

    long countByIsActive(boolean active);

    long countByIsVerified(boolean verified);

    long countByProvider(User.AuthProvider provider);

    long countByCreatedAtAfter(LocalDateTime date);
}
