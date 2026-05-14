package com.connectsphere.post.kafka;

import com.connectsphere.post.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostEventProducer Tests")
class PostEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PostEventProducer producer;

    @Test
    @DisplayName("sendPostCreatedEvent publishes the expected payload")
    void sendPostCreatedEvent_success() {
        Post post = Post.builder().postId("p1").authorId("u1").content(null).build();

        producer.sendPostCreatedEvent(post);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq("post-events"), org.mockito.Mockito.eq("p1"), captor.capture());
        assertThat(captor.getValue()).containsEntry("eventType", "POST_CREATED");
        assertThat(captor.getValue()).containsEntry("content", "");
    }

    @Test
    @DisplayName("sendPostDeletedEvent publishes delete payload")
    void sendPostDeletedEvent_success() {
        producer.sendPostDeletedEvent("p1");

        verify(kafkaTemplate).send("post-events", "p1", Map.of("eventType", "POST_DELETED", "postId", "p1"));
    }

    @Test
    @DisplayName("Producer swallows send failures")
    void sendPostCreatedEvent_handlesSendFailure() {
        Post post = Post.builder().postId("p1").authorId("u1").content("hello").build();
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate)
                .send(org.mockito.Mockito.eq("post-events"), org.mockito.Mockito.eq("p1"), org.mockito.ArgumentMatchers.anyMap());

        producer.sendPostCreatedEvent(post);
    }
}
