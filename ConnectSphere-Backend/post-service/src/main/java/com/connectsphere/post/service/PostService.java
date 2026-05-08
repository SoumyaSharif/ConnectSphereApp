package com.connectsphere.post.service;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.PostDto;
import com.connectsphere.post.entity.Post;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {
    PostDto createPost(String authorId, CreatePostRequest request);
    Page<PostDto> getHomeFeed(String requesterId, int page, int size);
    PostDto getPostById(String postId);
    Page<PostDto> getPostsByUser(String authorId, int page, int size);
    Page<PostDto> getPublicFeed(int page, int size);
    Page<PostDto> getFeedForUser(List<String> followeeIds, int page, int size);
    PostDto updatePost(String postId, String requesterId, CreatePostRequest request);
    void deletePost(String postId, String requesterId, String requesterRole);
    Page<PostDto> searchPosts(String query, int page, int size);
    void incrementLikes(String postId);
    void decrementLikes(String postId);
    void incrementComments(String postId);
    void decrementComments(String postId);
    void incrementShares(String postId);
    void changeVisibility(String postId, String requesterId, Post.Visibility visibility);
    long getPostCount(String authorId);
    long getTotalPostCount();
}
