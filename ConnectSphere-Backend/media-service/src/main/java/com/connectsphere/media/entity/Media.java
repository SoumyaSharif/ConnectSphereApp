package com.connectsphere.media.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String mediaId;
    @Column(name = "uploader_id", nullable = false) private String uploaderId;
    @Column(nullable = false) private String url;
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;
    private long sizeKb;
    private String mimeType;
    @Column(name = "linked_post_id") private String linkedPostId;
    @Column(name = "is_deleted") @Builder.Default private boolean isDeleted = false;
    @CreationTimestamp @Column(name = "uploaded_at", updatable = false) private LocalDateTime uploadedAt;
    public enum MediaType { IMAGE, VIDEO }
}
