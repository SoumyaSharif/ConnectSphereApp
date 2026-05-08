package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, String> {
    Optional<Like> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, Like.TargetType targetType);
    List<Like> findByTargetIdAndTargetType(String targetId, Like.TargetType targetType);
    List<Like> findByUserId(String userId);
    boolean existsByUserIdAndTargetIdAndTargetType(String userId, String targetId, Like.TargetType targetType);
    long countByTargetIdAndTargetType(String targetId, Like.TargetType targetType);
    void deleteByUserIdAndTargetIdAndTargetType(String userId, String targetId, Like.TargetType targetType);

    @Query("SELECT l.reactionType, COUNT(l) FROM Like l WHERE l.targetId = :targetId AND l.targetType = :targetType GROUP BY l.reactionType")
    List<Object[]> countByTargetIdAndTargetTypeGroupByReactionType(@Param("targetId") String targetId, @Param("targetType") Like.TargetType targetType);
}
