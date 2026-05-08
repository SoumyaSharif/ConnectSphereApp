package com.connectsphere.post.kafka;

import com.connectsphere.post.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPostCreatedEvent(Post post) {
        Map<String, Object> event = Map.of(
                "eventType", "POST_CREATED",
                "postId", post.getPostId(),
                "authorId", post.getAuthorId(),
                "content", post.getContent() != null ? post.getContent() : ""
        );
        try {
            kafkaTemplate.send("post-events", post.getPostId(), event);
            log.debug("Sent POST_CREATED event for post: {}", post.getPostId());
        } catch (Exception e) {
            log.warn("Failed to publish POST_CREATED event for {}: {}", post.getPostId(), e.getMessage());
        }
    }

    public void sendPostDeletedEvent(String postId) {
        Map<String, Object> event = Map.of(
                "eventType", "POST_DELETED",
                "postId", postId
        );
        try {
            kafkaTemplate.send("post-events", postId, event);
            log.debug("Sent POST_DELETED event for post: {}", postId);
        } catch (Exception e) {
            log.warn("Failed to publish POST_DELETED event for {}: {}", postId, e.getMessage());
        }
    }
}
