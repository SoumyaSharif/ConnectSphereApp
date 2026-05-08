package com.connectsphere.search.kafka;

import com.connectsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventConsumer {

    private final SearchService searchService;

    @KafkaListener(topics = "post-events", groupId = "search-group")
    public void onPostEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String postId = (String) event.get("postId");
            if ("POST_CREATED".equals(eventType)) {
                String content = (String) event.getOrDefault("content", "");
                searchService.indexPost(postId, content);
                log.debug("Indexed hashtags for post: {}", postId);
            } else if ("POST_DELETED".equals(eventType)) {
                searchService.removePostIndex(postId);
                log.debug("Removed index for post: {}", postId);
            }
        } catch (Exception e) {
            log.error("Error processing post event in search: {}", e.getMessage());
        }
    }
}
