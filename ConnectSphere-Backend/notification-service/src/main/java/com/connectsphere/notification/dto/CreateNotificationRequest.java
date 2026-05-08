package com.connectsphere.notification.dto;

import com.connectsphere.notification.entity.Notification;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    private String recipientId;
    private String actorId;
    private Notification.NotificationType type;
    private String message;
    private String targetId;
    private String targetType;
    private String deepLinkUrl;
}
