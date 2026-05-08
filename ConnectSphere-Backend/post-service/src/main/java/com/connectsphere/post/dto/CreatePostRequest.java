package com.connectsphere.post.dto;

import com.connectsphere.post.entity.Post;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private String content;
    private List<String> mediaUrls;
    private Post.Visibility visibility = Post.Visibility.PUBLIC;
}
