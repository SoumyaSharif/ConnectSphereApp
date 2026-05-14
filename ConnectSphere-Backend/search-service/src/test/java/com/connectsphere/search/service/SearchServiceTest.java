package com.connectsphere.search.service;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Tests")
class SearchServiceTest {

    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository postHashtagRepository;

    @InjectMocks private SearchService searchService;

    @Test
    @DisplayName("indexPost creates new hashtags and avoids duplicate post mappings")
    void indexPost_createsAndDeduplicates() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.empty());
        when(hashtagRepository.findByTag("spring")).thenReturn(Optional.of(
                Hashtag.builder().hashtagId("h2").tag("spring").postCount(4).build()
        ));
        when(hashtagRepository.save(any(Hashtag.class))).thenAnswer(invocation -> {
            Hashtag hashtag = invocation.getArgument(0);
            if (hashtag.getHashtagId() == null) {
                hashtag.setHashtagId("h1");
            }
            return hashtag;
        });
        when(postHashtagRepository.findByPostId("p1")).thenReturn(List.of(
                PostHashtag.builder().postId("p1").hashtagId("h1").build()
        ));

        searchService.indexPost("p1", "Learning #Java with #Spring and #java again");

        ArgumentCaptor<PostHashtag> captor = ArgumentCaptor.forClass(PostHashtag.class);
        verify(postHashtagRepository).save(captor.capture());
        assertThat(captor.getValue().getHashtagId()).isEqualTo("h2");
        verify(hashtagRepository, times(3)).save(any(Hashtag.class));
    }

    @Test
    @DisplayName("removePostIndex decrements counts and removes mappings")
    void removePostIndex_updatesCountsAndDeletesMappings() {
        Hashtag hashtag = Hashtag.builder().hashtagId("h1").tag("java").postCount(2).build();
        when(postHashtagRepository.findByPostId("p1")).thenReturn(List.of(
                PostHashtag.builder().postId("p1").hashtagId("h1").build()
        ));
        when(hashtagRepository.findById("h1")).thenReturn(Optional.of(hashtag));

        searchService.removePostIndex("p1");

        assertThat(hashtag.getPostCount()).isEqualTo(1);
        verify(hashtagRepository).save(hashtag);
        verify(postHashtagRepository).deleteByPostId("p1");
    }

    @Test
    @DisplayName("getPostsByHashtag normalizes tag casing")
    void getPostsByHashtag_normalizesTag() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.of(
                Hashtag.builder().hashtagId("h1").tag("java").build()
        ));
        when(postHashtagRepository.findByHashtagId("h1")).thenReturn(List.of(
                PostHashtag.builder().postId("p1").hashtagId("h1").build(),
                PostHashtag.builder().postId("p2").hashtagId("h1").build()
        ));

        List<String> result = searchService.getPostsByHashtag("JaVa");

        assertThat(result).containsExactly("p1", "p2");
    }

    @Test
    @DisplayName("getHashtagsForPost filters out missing hashtags")
    void getHashtagsForPost_filtersMissingHashtags() {
        when(postHashtagRepository.findByPostId("p1")).thenReturn(List.of(
                PostHashtag.builder().postId("p1").hashtagId("h1").build(),
                PostHashtag.builder().postId("p1").hashtagId("h2").build()
        ));
        when(hashtagRepository.findById("h1")).thenReturn(Optional.of(
                Hashtag.builder().hashtagId("h1").tag("java").build()
        ));
        when(hashtagRepository.findById("h2")).thenReturn(Optional.empty());

        List<String> result = searchService.getHashtagsForPost("p1");

        assertThat(result).containsExactly("java");
    }

    @Test
    @DisplayName("searchHashtags delegates to the repository")
    void searchHashtags_delegates() {
        List<Hashtag> expected = List.of(Hashtag.builder().tag("spring").build());
        when(hashtagRepository.findByTagContainingIgnoreCase("spr")).thenReturn(expected);

        List<Hashtag> result = searchService.searchHashtags("spr");

        assertThat(result).isSameAs(expected);
        verify(hashtagRepository).findByTagContainingIgnoreCase(eq("spr"));
    }

    @Test
    @DisplayName("indexPost ignores null or blank content")
    void indexPost_ignoresBlankContent() {
        searchService.indexPost("p1", null);
        searchService.indexPost("p1", "   ");

        verify(hashtagRepository, times(0)).findByTag(any());
        verify(postHashtagRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("removePostIndex never decrements below zero")
    void removePostIndex_floorsAtZero() {
        Hashtag hashtag = Hashtag.builder().hashtagId("h1").tag("java").postCount(0).build();
        when(postHashtagRepository.findByPostId("p1")).thenReturn(List.of(
                PostHashtag.builder().postId("p1").hashtagId("h1").build()
        ));
        when(hashtagRepository.findById("h1")).thenReturn(Optional.of(hashtag));

        searchService.removePostIndex("p1");

        assertThat(hashtag.getPostCount()).isZero();
    }

    @Test
    @DisplayName("getTrendingHashtags delegates to repository")
    void getTrendingHashtags_delegates() {
        List<Hashtag> expected = List.of(Hashtag.builder().tag("space").build());
        when(hashtagRepository.findTrendingHashtags(any())).thenReturn(expected);

        assertThat(searchService.getTrendingHashtags(5)).isSameAs(expected);
    }

    @Test
    @DisplayName("getPostsByHashtag returns empty list when tag is unknown")
    void getPostsByHashtag_unknownTag() {
        when(hashtagRepository.findByTag("missing")).thenReturn(Optional.empty());

        assertThat(searchService.getPostsByHashtag("missing")).isEmpty();
    }

    @Test
    @DisplayName("getHashtagCount delegates to repository")
    void getHashtagCount_delegates() {
        when(hashtagRepository.count()).thenReturn(9L);

        assertThat(searchService.getHashtagCount()).isEqualTo(9L);
    }
}
