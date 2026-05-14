package com.connectsphere.notification.kafka;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventConsumer Tests")
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    @DisplayName("LIKE events map to notification service payload")
    void onNotificationEvent_like() {
        consumer.onNotificationEvent(Map.of(
                "eventType", "LIKE",
                "recipientId", "u2",
                "actorId", "u1",
                "targetId", "p1",
                "targetType", "POST",
                "message", "liked your post"
        ));

        verify(notificationService).createNotification(
                "u2", "u1", Notification.NotificationType.LIKE,
                "liked your post", "p1", "POST", "/posts/p1");
    }

    @Test
    @DisplayName("Unknown event types fall back to SYSTEM")
    void onNotificationEvent_unknownFallsBackToSystem() {
        consumer.onNotificationEvent(Map.of(
                "eventType", "WHATEVER",
                "recipientId", "u2",
                "actorId", "u1",
                "targetId", "p1",
                "targetType", "POST",
                "message", "system note"
        ));

        verify(notificationService).createNotification(
                "u2", "u1", Notification.NotificationType.SYSTEM,
                "system note", "p1", "POST", "/posts/p1");
    }

    @Test
    @DisplayName("Consumer swallows notification creation errors")
    void onNotificationEvent_handlesExceptions() {
        doThrow(new RuntimeException("boom")).when(notificationService).createNotification(
                "u2", "u1", Notification.NotificationType.FOLLOW, "started following you", "u2", "USER", "/posts/u2");

        consumer.onNotificationEvent(Map.of(
                "eventType", "FOLLOW",
                "recipientId", "u2",
                "actorId", "u1",
                "targetId", "u2",
                "targetType", "USER",
                "message", "started following you"
        ));
    }

    @Test
    @DisplayName("Malformed events are ignored safely")
    void onNotificationEvent_malformedEvent() {
        consumer.onNotificationEvent(Map.of("eventType", 123));

        verifyNoInteractions(notificationService);
    }
}
