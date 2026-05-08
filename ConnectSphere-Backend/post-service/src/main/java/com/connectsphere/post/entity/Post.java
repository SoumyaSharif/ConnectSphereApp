package com.connectsphere.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "post_id", updatable = false, nullable = false)
    private String postId;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_urls", columnDefinition = "LONGTEXT")
    private String mediaUrls; // JSON array stored as string

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostType postType = PostType.TEXT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "likes_count")
    @Builder.Default
    private long likesCount = 0;

    @Column(name = "comments_count")
    @Builder.Default
    private long commentsCount = 0;

    @Column(name = "shares_count")
    @Builder.Default
    private long sharesCount = 0;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PostType { TEXT, MEDIA }
    public enum Visibility { PUBLIC, FOLLOWERS, PRIVATE }
}
