package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationResource Tests")
class NotificationResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private Notification buildNotification() {
        return Notification.builder()
                .notificationId("n1")
                .recipientId("u1")
                .actorId("u2")
                .type(Notification.NotificationType.LIKE)
                .message("liked your post")
                .targetId("p1")
                .targetType("POST")
                .deepLinkUrl("/post/p1")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications returns paged notifications")
    void getMyNotifications_success() throws Exception {
        when(notificationService.getByRecipient("u1", 0, 20))
                .thenReturn(new PageImpl<>(List.of(buildNotification())));

        mockMvc.perform(get("/api/v1/notifications").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationId").value("n1"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count returns unread count")
    void getUnreadCount_success() throws Exception {
        when(notificationService.getUnreadCount("u1")).thenReturn(4L);

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read marks one notification as read")
    void markAsRead_success() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/n1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Marked as read"));

        verify(notificationService).markAsRead("n1");
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/read-all marks all as read")
    void markAllRead_success() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All marked as read"));

        verify(notificationService).markAllRead("u1");
    }

    @Test
    @DisplayName("DELETE /api/v1/notifications/{id} deletes a notification")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/n1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted"));

        verify(notificationService).deleteNotification("n1");
    }

    @Test
    @DisplayName("POST /api/v1/notifications/broadcast rejects non-admin users")
    void broadcast_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/broadcast")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientIds", List.of("u1"),
                                "message", "hello"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/broadcast sends bulk notifications for admins")
    void broadcast_success() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/broadcast")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientIds", List.of("u1", "u2"),
                                "message", "hello"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Broadcast sent"));

        verify(notificationService).sendBulkNotification(
                eq(List.of("u1", "u2")),
                eq("SYSTEM"),
                eq(Notification.NotificationType.BROADCAST),
                eq("hello"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/internal creates a notification")
    void createInternal_success() throws Exception {
        Notification notification = buildNotification();
        when(notificationService.createNotification("u1", "u2", Notification.NotificationType.LIKE,
                "liked your post", "p1", "POST", "/post/p1")).thenReturn(notification);

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setRecipientId("u1");
        request.setActorId("u2");
        request.setType(Notification.NotificationType.LIKE);
        request.setMessage("liked your post");
        request.setTargetId("p1");
        request.setTargetType("POST");
        request.setDeepLinkUrl("/post/p1");

        mockMvc.perform(post("/api/v1/notifications/internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value("n1"));
    }
}
