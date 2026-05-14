package com.connectsphere.search.kafka;

import com.connectsphere.search.service.SearchService;
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
@DisplayName("PostEventConsumer Tests")
class PostEventConsumerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private PostEventConsumer consumer;

    @Test
    @DisplayName("POST_CREATED indexes a post with content fallback")
    void onPostEvent_created() {
        consumer.onPostEvent(Map.of(
                "eventType", "POST_CREATED",
                "postId", "p1",
                "content", "hello #space"
        ));

        verify(searchService).indexPost("p1", "hello #space");
    }

    @Test
    @DisplayName("POST_DELETED removes the search index")
    void onPostEvent_deleted() {
        consumer.onPostEvent(Map.of(
                "eventType", "POST_DELETED",
                "postId", "p1"
        ));

        verify(searchService).removePostIndex("p1");
    }

    @Test
    @DisplayName("Unknown events are ignored")
    void onPostEvent_unknownIgnored() {
        consumer.onPostEvent(Map.of(
                "eventType", "IGNORED",
                "postId", "p1"
        ));

        verifyNoInteractions(searchService);
    }

    @Test
    @DisplayName("Consumer swallows service errors")
    void onPostEvent_handlesExceptions() {
        doThrow(new RuntimeException("boom")).when(searchService).indexPost("p1", "");

        consumer.onPostEvent(Map.of(
                "eventType", "POST_CREATED",
                "postId", "p1"
        ));
    }
}
