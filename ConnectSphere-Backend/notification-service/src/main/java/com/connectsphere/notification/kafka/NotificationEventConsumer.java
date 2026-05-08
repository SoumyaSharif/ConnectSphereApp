package com.connectsphere.notification.kafka;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer listening to notification-events and post-events topics.
 * Creates notifications based on event type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notification-events", groupId = "notification-group")
    public void onNotificationEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String recipientId = (String) event.get("recipientId");
            String actorId = (String) event.get("actorId");
            String targetId = (String) event.get("targetId");
            String targetType = (String) event.get("targetType");
            String message = (String) event.get("message");

            Notification.NotificationType type = switch (eventType) {
                case "LIKE" -> Notification.NotificationType.LIKE;
                case "COMMENT" -> Notification.NotificationType.COMMENT;
                case "REPLY" -> Notification.NotificationType.REPLY;
                case "FOLLOW" -> Notification.NotificationType.FOLLOW;
                case "MENTION" -> Notification.NotificationType.MENTION;
                default -> Notification.NotificationType.SYSTEM;
            };

            notificationService.createNotification(recipientId, actorId, type, message, targetId, targetType,
                    "/posts/" + targetId);
            log.debug("Processed notification event: {} for recipient: {}", eventType, recipientId);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage());
        }
    }
}
