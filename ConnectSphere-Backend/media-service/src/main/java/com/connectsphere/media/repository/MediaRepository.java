package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, String> {
    List<Media> findByUploaderIdAndIsDeletedFalse(String uploaderId);
    List<Media> findByLinkedPostIdAndIsDeletedFalse(String linkedPostId);
}
