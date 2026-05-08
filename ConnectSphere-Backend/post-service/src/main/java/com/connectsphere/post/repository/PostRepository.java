package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    Page<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(String authorId, Pageable pageable);

    Page<Post> findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(Post.Visibility visibility, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND " +
           "(p.visibility = 'PUBLIC' OR p.authorId = :authorId) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findHomeFeed(@Param("authorId") String authorId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.authorId IN :authorIds AND p.isDeleted = false " +
           "AND (p.visibility = 'PUBLIC' OR p.visibility = 'FOLLOWERS') " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findFeedByAuthorIds(@Param("authorIds") List<String> authorIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND " +
           "(LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchByContent(@Param("query") String query, Pageable pageable);

    long countByAuthorIdAndIsDeletedFalse(String authorId);

    long countByIsDeletedFalse();

    List<Post> findByAuthorIdAndIsDeletedFalse(String authorId);
}
