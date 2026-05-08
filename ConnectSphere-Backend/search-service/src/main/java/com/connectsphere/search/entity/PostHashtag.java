package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_hashtags",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "hashtag_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostHashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "post_id", nullable = false) private String postId;
    @Column(name = "hashtag_id", nullable = false) private String hashtagId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
