package com.connectsphere.media.service;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Tests")
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private RestTemplate loadBalancedRestTemplate;
    @Mock private RestTemplate defaultRestTemplate;

    @InjectMocks private MediaService mediaService;

    @Test
    @DisplayName("uploadMedia saves the expected media metadata")
    void uploadMedia_savesMedia() {
        Media saved = Media.builder()
                .mediaId("m1")
                .uploaderId("u1")
                .url("https://cdn/image.png")
                .mediaType(Media.MediaType.IMAGE)
                .sizeKb(120)
                .mimeType("image/png")
                .linkedPostId("p1")
                .build();
        when(mediaRepository.save(any(Media.class))).thenReturn(saved);

        Media result = mediaService.uploadMedia("u1", "https://cdn/image.png", Media.MediaType.IMAGE, 120, "image/png", "p1");

        assertThat(result.getMediaId()).isEqualTo("m1");
        ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(captor.capture());
        assertThat(captor.getValue().getUploaderId()).isEqualTo("u1");
        assertThat(captor.getValue().getLinkedPostId()).isEqualTo("p1");
    }

    @Test
    @DisplayName("deleteMedia marks existing media as deleted")
    void deleteMedia_marksDeleted() {
        Media media = Media.builder().mediaId("m1").isDeleted(false).build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));

        mediaService.deleteMedia("m1");

        assertThat(media.isDeleted()).isTrue();
        verify(mediaRepository).save(media);
    }

    @Test
    @DisplayName("getMediaByPost delegates to repository")
    void getMediaByPost_returnsRepositoryResults() {
        List<Media> expected = List.of(buildMedia("m1", "p1"));
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalse("p1")).thenReturn(expected);

        assertThat(mediaService.getMediaByPost("p1")).isSameAs(expected);
    }

    @Test
    @DisplayName("getMediaById returns media when present")
    void getMediaById_found() {
        Media media = buildMedia("m1", "p1");
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));

        assertThat(mediaService.getMediaById("m1")).isSameAs(media);
    }

    @Test
    @DisplayName("getMediaById throws when media is missing")
    void getMediaById_missing() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMediaById("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Media not found");
    }

    @Test
    @DisplayName("createStory persists a story with expiration")
    void createStory_savesStory() {
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Story result = mediaService.createStory("u1", "https://cdn/story.png", "hello", Media.MediaType.IMAGE);

        assertThat(result.getAuthorId()).isEqualTo("u1");
        assertThat(result.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("getActiveStoriesByUser delegates to repository")
    void getActiveStoriesByUser_returnsStories() {
        List<Story> expected = List.of(Story.builder().storyId("s1").authorId("u1").build());
        when(storyRepository.findActiveStoriesByAuthorId(eq("u1"), any())).thenReturn(expected);

        assertThat(mediaService.getActiveStoriesByUser("u1")).isSameAs(expected);
    }

    @Test
    @DisplayName("getActiveStoriesByFollowees returns empty list for blank ids")
    void getActiveStoriesByFollowees_emptyIds() {
        List<Story> result = mediaService.getActiveStoriesByFollowees(Arrays.asList(" ", null, "\t"));

        assertThat(result).isEmpty();
        verify(storyRepository, never()).findActiveStoriesByAuthorIds(any(), any());
    }

    @Test
    @DisplayName("getActiveStoriesByFollowees trims duplicates before querying")
    void getActiveStoriesByFollowees_normalizesIds() {
        Story story = Story.builder().storyId("s1").authorId("u2").isActive(true).build();
        when(storyRepository.findActiveStoriesByAuthorIds(eq(List.of("u2", "u3")), any()))
                .thenReturn(List.of(story));

        List<Story> result = mediaService.getActiveStoriesByFollowees(Arrays.asList("u2", " u3 ", "u2", "", null));

        assertThat(result).containsExactly(story);
    }

    @Test
    @DisplayName("getActiveStoriesFeed returns empty when no followee ids are resolved")
    void getActiveStoriesFeed_returnsEmptyWhenFollowServiceUnavailable() {
        List<Story> result = mediaService.getActiveStoriesFeed("viewer");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getActiveStoriesFeed uses load-balanced follow service when available")
    void getActiveStoriesFeed_usesLoadBalancedService() {
        Story story = Story.builder().storyId("s1").authorId("u2").build();
        when(loadBalancedRestTemplate.getForObject("http://follow-service/api/v1/follows/{userId}/following", String[].class, "viewer"))
                .thenReturn(new String[]{"u2", "u3"});
        when(storyRepository.findActiveStoriesByAuthorIds(eq(List.of("u2", "u3")), any())).thenReturn(List.of(story));

        List<Story> result = mediaService.getActiveStoriesFeed("viewer");

        assertThat(result).containsExactly(story);
    }

    @Test
    @DisplayName("getActiveStoriesFeed falls back to default rest template")
    void getActiveStoriesFeed_fallsBackToDefaultTemplate() {
        Story story = Story.builder().storyId("s1").authorId("u2").build();
        lenient().when(loadBalancedRestTemplate.getForObject("http://follow-service/api/v1/follows/{userId}/following", String[].class, "viewer"))
                .thenThrow(new RestClientException("down"));
        when(defaultRestTemplate.getForObject(contains("/api/v1/follows/{userId}/following"), eq(String[].class), eq("viewer")))
                .thenReturn(new String[]{"u2"});
        when(storyRepository.findActiveStoriesByAuthorIds(eq(List.of("u2")), any())).thenReturn(List.of(story));

        List<Story> result = mediaService.getActiveStoriesFeed("viewer");

        assertThat(result).containsExactly(story);
    }

    @Test
    @DisplayName("viewStory increments the story view count")
    void viewStory_incrementsViewCount() {
        Story story = Story.builder().storyId("s1").viewsCount(2).isActive(true).build();
        when(storyRepository.findById("s1")).thenReturn(Optional.of(story));

        mediaService.viewStory("s1");

        assertThat(story.getViewsCount()).isEqualTo(3);
        verify(storyRepository).save(story);
    }

    @Test
    @DisplayName("deleteStory throws when requester is not the author")
    void deleteStory_forbidden() {
        Story story = Story.builder().storyId("s1").authorId("owner").isActive(true).build();
        when(storyRepository.findById("s1")).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> mediaService.deleteStory("s1", "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("deleteStory deactivates story for the author")
    void deleteStory_ownerSuccess() {
        Story story = Story.builder().storyId("s1").authorId("owner").isActive(true).build();
        when(storyRepository.findById("s1")).thenReturn(Optional.of(story));

        mediaService.deleteStory("s1", "owner");

        assertThat(story.isActive()).isFalse();
        verify(storyRepository).save(story);
    }

    @Test
    @DisplayName("expireOldStories delegates to the repository purge operation")
    void expireOldStories_delegates() {
        when(storyRepository.expireStories(any())).thenReturn(2);

        mediaService.expireOldStories();

        verify(storyRepository).expireStories(any());
    }

    private Media buildMedia(String mediaId, String linkedPostId) {
        return Media.builder()
                .mediaId(mediaId)
                .uploaderId("u1")
                .url("https://cdn/image.png")
                .mediaType(Media.MediaType.IMAGE)
                .linkedPostId(linkedPostId)
                .build();
    }
}
