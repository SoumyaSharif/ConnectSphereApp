package com.connectsphere.post.dto;

import com.connectsphere.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
    private String postId;
    private String authorId;
    private String content;
    private List<String> mediaUrls;
    private Post.PostType postType;
    private Post.Visibility visibility;
    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
