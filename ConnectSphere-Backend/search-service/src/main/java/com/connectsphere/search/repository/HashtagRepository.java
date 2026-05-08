package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, String> {
    Optional<Hashtag> findByTag(String tag);
    List<Hashtag> findByTagContainingIgnoreCase(String query);
    @Query("SELECT h FROM Hashtag h ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(Pageable pageable);
}
