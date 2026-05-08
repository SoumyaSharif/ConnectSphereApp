package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, String> {
    List<PostHashtag> findByHashtagId(String hashtagId);
    List<PostHashtag> findByPostId(String postId);
    void deleteByPostId(String postId);
}
