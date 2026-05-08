package com.connectsphere.like.service.impl;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl {

    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public Like likeTarget(String userId, String targetId, Like.TargetType targetType, Like.ReactionType reactionType) {
        if (likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)) {
            throw new RuntimeException("Already reacted to this target");
        }
        Like like = Like.builder()
                .userId(userId).targetId(targetId)
                .targetType(targetType).reactionType(reactionType)
                .build();
        like = likeRepository.save(like);
        notifyCountChange(targetId, targetType, true);
        sendLikeNotification(userId, targetId, targetType);
        return like;
    }

    @Transactional
    public void unlikeTarget(String userId, String targetId, Like.TargetType targetType) {
        if (!likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)) {
            throw new RuntimeException("No reaction found");
        }
        likeRepository.deleteByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        notifyCountChange(targetId, targetType, false);
    }

    @Transactional
    public Like changeReaction(String userId, String targetId, Like.TargetType targetType, Like.ReactionType newType) {
        Like like = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new RuntimeException("No reaction found"));
        like.setReactionType(newType);
        return likeRepository.save(like);
    }

    public boolean hasLiked(String userId, String targetId, Like.TargetType targetType) {
        return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
    }

    public long getLikeCount(String targetId, Like.TargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    public Map<String, Long> getReactionSummary(String targetId, Like.TargetType targetType) {
        List<Object[]> rows = likeRepository.countByTargetIdAndTargetTypeGroupByReactionType(targetId, targetType);
        return rows.stream().collect(Collectors.toMap(
                r -> r[0].toString(), r -> (Long) r[1]));
    }

    public List<Like> getLikesByTarget(String targetId, Like.TargetType targetType) {
        return likeRepository.findByTargetIdAndTargetType(targetId, targetType);
    }

    public List<Like> getLikesByUser(String userId) {
        return likeRepository.findByUserId(userId);
    }

    public long getTotalLikeCount() {
        return likeRepository.count();
    }

    private void notifyCountChange(String targetId, Like.TargetType targetType, boolean increment) {
        if (targetType == Like.TargetType.POST) {
            String path = increment ? "/api/v1/posts/{id}/likes/increment" : "/api/v1/posts/{id}/likes/decrement";
            try {
                restTemplate.patchForObject("http://post-service" + path, null, Void.class, targetId);
            } catch (Exception e) {
                log.warn("Failed to update post like count: {}", e.getMessage());
            }
            return;
        }

        if (targetType == Like.TargetType.COMMENT) {
            String path = increment ? "/api/v1/comments/{id}/likes/increment" : "/api/v1/comments/{id}/likes/decrement";
            try {
                restTemplate.patchForObject("http://comment-service" + path, null, Void.class, targetId);
            } catch (Exception e) {
                log.warn("Failed to update comment like count: {}", e.getMessage());
            }
        }
    }

    private void sendLikeNotification(String actorId, String targetId, Like.TargetType targetType) {
        try {
            if (targetType == Like.TargetType.POST) {
                @SuppressWarnings("unchecked")
                Map<String, Object> post = restTemplate.getForObject(
                        "http://post-service/api/v1/posts/{id}",
                        Map.class,
                        targetId
                );

                String recipientId = post != null ? (String) post.get("authorId") : null;
                if (recipientId == null || recipientId.equals(actorId)) {
                    return;
                }

                createNotification(recipientId, actorId, "LIKE", "liked your post.", targetId, "POST", "/post/" + targetId);
                return;
            }

            if (targetType == Like.TargetType.COMMENT) {
                @SuppressWarnings("unchecked")
                Map<String, Object> comment = restTemplate.getForObject(
                        "http://comment-service/api/v1/comments/{id}",
                        Map.class,
                        targetId
                );

                String recipientId = comment != null ? (String) comment.get("authorId") : null;
                String postId = comment != null ? (String) comment.get("postId") : null;
                if (recipientId == null || recipientId.equals(actorId) || postId == null) {
                    return;
                }

                createNotification(recipientId, actorId, "LIKE", "liked your comment.", postId, "COMMENT", "/post/" + postId);
            }
        } catch (Exception e) {
            log.warn("Failed to create like notification: {}", e.getMessage());
        }
    }

    private void createNotification(String recipientId,
                                    String actorId,
                                    String type,
                                    String message,
                                    String targetId,
                                    String targetType,
                                    String deepLinkUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", type);
        payload.put("message", message);
        payload.put("targetId", targetId);
        payload.put("targetType", targetType);
        payload.put("deepLinkUrl", deepLinkUrl);
        restTemplate.postForObject("http://notification-service/api/v1/notifications/internal", payload, Void.class);
    }
}
