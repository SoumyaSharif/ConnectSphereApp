package com.connectsphere.search.service;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    @Transactional
    public void indexPost(String postId, String content) {
        if (content == null || content.isBlank()) return;
        Matcher m = HASHTAG_PATTERN.matcher(content);
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            Hashtag hashtag = hashtagRepository.findByTag(tag).orElse(null);
            if (hashtag == null) {
                hashtag = hashtagRepository.save(Hashtag.builder().tag(tag).postCount(1).lastUsedAt(LocalDateTime.now()).build());
            } else {
                hashtag.setPostCount(hashtag.getPostCount() + 1);
                hashtag.setLastUsedAt(LocalDateTime.now());
                hashtag = hashtagRepository.save(hashtag);
            }
            final String hashtagId = hashtag.getHashtagId();

            boolean exists = postHashtagRepository.findByPostId(postId).stream()
                .anyMatch(ph -> ph.getHashtagId().equals(hashtagId));

            if (!exists) {
                postHashtagRepository.save(
                    PostHashtag.builder()
                        .postId(postId)
                        .hashtagId(hashtagId)
                        .build()
                );
            }
        }
    }

    @Transactional
    public void removePostIndex(String postId) {
        List<PostHashtag> postHashtags = postHashtagRepository.findByPostId(postId);
        postHashtags.forEach(ph -> hashtagRepository.findById(ph.getHashtagId()).ifPresent(h -> {
            h.setPostCount(Math.max(0, h.getPostCount() - 1));
            hashtagRepository.save(h);
        }));
        postHashtagRepository.deleteByPostId(postId);
    }

    public List<Hashtag> getTrendingHashtags(int limit) {
        return hashtagRepository.findTrendingHashtags(PageRequest.of(0, limit));
    }

    public List<Hashtag> searchHashtags(String query) {
        return hashtagRepository.findByTagContainingIgnoreCase(query);
    }

    public List<String> getPostsByHashtag(String tag) {
        return hashtagRepository.findByTag(tag.toLowerCase())
                .map(h -> postHashtagRepository.findByHashtagId(h.getHashtagId())
                        .stream().map(PostHashtag::getPostId).collect(Collectors.toList()))
                .orElse(List.of());
    }

    public List<String> getHashtagsForPost(String postId) {
        return postHashtagRepository.findByPostId(postId).stream()
                .map(ph -> hashtagRepository.findById(ph.getHashtagId())
                        .map(Hashtag::getTag).orElse(null))
                .filter(t -> t != null)
                .collect(Collectors.toList());
    }

    public long getHashtagCount() {
        return hashtagRepository.count();
    }
}
