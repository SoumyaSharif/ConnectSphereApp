package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationResource {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications for current user (USER)")
    @GetMapping
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId, page, size));
    }

    @Operation(summary = "Get unread notification count (USER)")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @Operation(summary = "Mark a notification as read (USER)")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @Operation(summary = "Mark all notifications as read (USER)")
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(@RequestHeader("X-User-Id") String userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    @Operation(summary = "Delete a notification (USER)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @Operation(summary = "Send bulk broadcast notification (ADMIN)")
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcast(
            @RequestHeader("X-User-Role") String role,
            @RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) body.get("recipientIds");
        String message = (String) body.get("message");
        notificationService.sendBulkNotification(recipients, "SYSTEM",
                Notification.NotificationType.BROADCAST, message);
        return ResponseEntity.ok(Map.of("message", "Broadcast sent"));
    }

    @PostMapping("/internal")
    public ResponseEntity<Notification> createInternal(@RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(
                notificationService.createNotification(
                        request.getRecipientId(),
                        request.getActorId(),
                        request.getType(),
                        request.getMessage(),
                        request.getTargetId(),
                        request.getTargetType(),
                        request.getDeepLinkUrl()
                )
        );
    }
}
