package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, String> {
    @Query("""
            SELECT s
            FROM Story s
            WHERE s.authorId = :authorId
              AND s.isActive = true
              AND s.expiresAt > :now
            ORDER BY s.createdAt DESC
            """)
    List<Story> findActiveStoriesByAuthorId(@Param("authorId") String authorId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT s
            FROM Story s
            WHERE s.authorId IN :authorIds
              AND s.isActive = true
              AND s.expiresAt > :now
            ORDER BY s.createdAt DESC
            """)
    List<Story> findActiveStoriesByAuthorIds(@Param("authorIds") List<String> authorIds, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Story s SET s.isActive = false WHERE s.expiresAt < :now AND s.isActive = true")
    int expireStories(@Param("now") LocalDateTime now);
}
