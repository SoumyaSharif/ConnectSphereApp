package com.connectsphere.notification.service;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(String recipientId, String actorId,
                                           Notification.NotificationType type, String message,
                                           String targetId, String targetType, String deepLinkUrl) {
        Notification n = Notification.builder()
                .recipientId(recipientId).actorId(actorId).type(type)
                .message(message).targetId(targetId).targetType(targetType)
                .deepLinkUrl(deepLinkUrl).build();
        return notificationRepository.save(n);
    }

    @Transactional
    public void sendBulkNotification(List<String> recipientIds, String actorId,
                                     Notification.NotificationType type, String message) {
        recipientIds.forEach(rid -> createNotification(rid, actorId, type, message, null, null, null));
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(String recipientId) {
        notificationRepository.markAllReadByRecipientId(recipientId);
    }

    public Page<Notification> getByRecipient(String recipientId, int page, int size) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size));
    }

    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
