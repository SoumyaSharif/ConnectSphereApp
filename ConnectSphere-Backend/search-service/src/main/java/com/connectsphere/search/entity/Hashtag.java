package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hashtags", uniqueConstraints = @UniqueConstraint(columnNames = "tag"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String hashtagId;
    @Column(nullable = false, unique = true, length = 100) private String tag;
    @Column(name = "post_count") @Builder.Default private long postCount = 0;
    @Column(name = "last_used_at") private LocalDateTime lastUsedAt;
}
