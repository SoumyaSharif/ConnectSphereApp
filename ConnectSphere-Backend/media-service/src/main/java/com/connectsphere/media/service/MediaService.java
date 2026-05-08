package com.connectsphere.media.service;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StoryRepository storyRepository;
    @Qualifier("loadBalancedRestTemplate")
    private final RestTemplate loadBalancedRestTemplate;
    @Qualifier("defaultRestTemplate")
    private final RestTemplate defaultRestTemplate;

    // ---- Media ----
    @Transactional
    public Media uploadMedia(String uploaderId, String url, Media.MediaType type,
                             long sizeKb, String mimeType, String linkedPostId) {
        return mediaRepository.save(Media.builder()
                .uploaderId(uploaderId).url(url).mediaType(type)
                .sizeKb(sizeKb).mimeType(mimeType).linkedPostId(linkedPostId).build());
    }

    public List<Media> getMediaByPost(String postId) {
        return mediaRepository.findByLinkedPostIdAndIsDeletedFalse(postId);
    }

    public Media getMediaById(String mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
    }

    @Transactional
    public void deleteMedia(String mediaId) {
        mediaRepository.findById(mediaId).ifPresent(m -> { m.setDeleted(true); mediaRepository.save(m); });
    }

    // ---- Stories ----
    @Transactional
    public Story createStory(String authorId, String mediaUrl, String caption, Media.MediaType mediaType) {
        return storyRepository.save(Story.builder()
                .authorId(authorId).mediaUrl(mediaUrl).caption(caption)
                .mediaType(mediaType)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());
    }

    public List<Story> getActiveStoriesByUser(String userId) {
        return storyRepository.findActiveStoriesByAuthorId(userId, LocalDateTime.now());
    }

    public List<Story> getActiveStoriesByFollowees(List<String> followeeIds) {
        List<String> normalizedIds = normalizeUserIds(followeeIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        return storyRepository.findActiveStoriesByAuthorIds(normalizedIds, LocalDateTime.now());
    }

    public List<Story> getActiveStoriesFeed(String userId) {
        return getActiveStoriesByFollowees(fetchFollowingIds(userId));
    }

    @Transactional
    public void viewStory(String storyId) {
        storyRepository.findById(storyId).ifPresent(s -> {
            s.setViewsCount(s.getViewsCount() + 1);
            storyRepository.save(s);
        });
    }

    @Transactional
    public void deleteStory(String storyId, String requesterId) {
        storyRepository.findById(storyId).ifPresent(s -> {
            if (!s.getAuthorId().equals(requesterId)) throw new RuntimeException("Forbidden");
            s.setActive(false);
            storyRepository.save(s);
        });
    }

    // Scheduled every 5 minutes — purge expired stories
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireOldStories() {
        int expired = storyRepository.expireStories(LocalDateTime.now());
        if (expired > 0) log.info("Expired {} stories", expired);
    }

    private List<String> fetchFollowingIds(String userId) {
        String[] response = tryFetchFollowingIds(loadBalancedRestTemplate,
                "http://follow-service/api/v1/follows/{userId}/following",
                userId);
        if (response != null) {
            return normalizeUserIds(Arrays.asList(response));
        }

        response = tryFetchFollowingIds(defaultRestTemplate,
                "http://localhost:8085/api/v1/follows/{userId}/following",
                userId);
        if (response != null) {
            return normalizeUserIds(Arrays.asList(response));
        }

        log.warn("Falling back to an empty story feed because follow-service is unavailable for user {}", userId);
        return List.of();
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        return userIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String[] tryFetchFollowingIds(RestTemplate restTemplate, String url, String userId) {
        try {
            return restTemplate.getForObject(url, String[].class, userId);
        } catch (RestClientException ex) {
            log.warn("Failed to fetch following list for user {} via {}", userId, url);
            return null;
        }
    }
}
