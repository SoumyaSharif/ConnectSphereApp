package com.connectsphere.follow.repository;

import com.connectsphere.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, String> {
    Optional<Follow> findByFollowerIdAndFolloweeId(String followerId, String followeeId);
    List<Follow> findByFollowerIdAndStatus(String followerId, Follow.FollowStatus status);
    List<Follow> findByFolloweeIdAndStatus(String followeeId, Follow.FollowStatus status);
    boolean existsByFollowerIdAndFolloweeId(String followerId, String followeeId);
    long countByFollowerIdAndStatus(String followerId, Follow.FollowStatus status);
    long countByFolloweeIdAndStatus(String followeeId, Follow.FollowStatus status);
    void deleteByFollowerIdAndFolloweeId(String followerId, String followeeId);

    long countByStatus(Follow.FollowStatus status);

    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :userId AND f.status = 'ACTIVE' " +
           "AND f.followeeId IN (SELECT f2.followerId FROM Follow f2 WHERE f2.followeeId = :userId AND f2.status = 'ACTIVE')")
    List<String> findMutualFollows(@Param("userId") String userId);

    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :userId AND f.status = 'ACTIVE'")
    List<String> findFolloweeIds(@Param("userId") String userId);
}
