package com.connectsphere.notification.service;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationService notificationService;

    @Test
    @DisplayName("createNotification saves a notification with the given payload")
    void createNotification_savesNotification() {
        Notification saved = Notification.builder()
                .notificationId("n1")
                .recipientId("u2")
                .actorId("u1")
                .type(Notification.NotificationType.LIKE)
                .message("liked your post")
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.createNotification(
                "u2", "u1", Notification.NotificationType.LIKE, "liked your post", "p1", "POST", "/posts/p1"
        );

        assertThat(result.getNotificationId()).isEqualTo("n1");
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo("u2");
        assertThat(captor.getValue().getDeepLinkUrl()).isEqualTo("/posts/p1");
    }

    @Test
    @DisplayName("sendBulkNotification creates one notification per recipient")
    void sendBulkNotification_createsForEachRecipient() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendBulkNotification(
                List.of("u1", "u2"), "admin", Notification.NotificationType.BROADCAST, "System maintenance"
        );

        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead updates existing notification only")
    void markAsRead_updatesExistingNotification() {
        Notification notification = Notification.builder().notificationId("n1").isRead(false).build();
        when(notificationRepository.findById("n1")).thenReturn(Optional.of(notification));

        notificationService.markAsRead("n1");

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead does nothing when notification is missing")
    void markAsRead_missingNotification() {
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        notificationService.markAsRead("missing");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAllRead delegates to repository")
    void markAllRead_delegates() {
        notificationService.markAllRead("u1");

        verify(notificationRepository).markAllReadByRecipientId("u1");
    }

    @Test
    @DisplayName("getByRecipient returns repository page")
    void getByRecipient_returnsPage() {
        Page<Notification> page = new PageImpl<>(List.of(Notification.builder().notificationId("n1").build()));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq("u1"), any())).thenReturn(page);

        Page<Notification> result = notificationService.getByRecipient("u1", 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("unread count and delete delegate to repository")
    void unreadCountAndDelete_delegate() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse("u1")).thenReturn(3L);

        long count = notificationService.getUnreadCount("u1");
        notificationService.deleteNotification("n1");

        assertThat(count).isEqualTo(3);
        verify(notificationRepository).deleteById("n1");
    }

    @Test
    @DisplayName("getByRecipient passes page and size through as a PageRequest")
    void getByRecipient_usesRequestedPagination() {
        Page<Notification> page = new PageImpl<>(List.of());
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq("u1"), any())).thenReturn(page);

        notificationService.getByRecipient("u1", 2, 25);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq("u1"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
    }
}
