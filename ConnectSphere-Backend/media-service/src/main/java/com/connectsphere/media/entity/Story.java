package com.connectsphere.media.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "stories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String storyId;
    @Column(name = "author_id", nullable = false) private String authorId;
    @Lob
    @Column(name = "media_url", nullable = false, columnDefinition = "LONGTEXT")
    private String mediaUrl;
    @Column(columnDefinition = "TEXT")
    private String caption;
    @Enumerated(EnumType.STRING)
    private Media.MediaType mediaType;
    @Column(name = "views_count") @Builder.Default private long viewsCount = 0;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "is_active") @Builder.Default private boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
