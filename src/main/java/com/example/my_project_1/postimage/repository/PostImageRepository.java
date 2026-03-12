package com.example.my_project_1.postimage.repository;

import com.example.my_project_1.postimage.domain.ImageStatus;
import com.example.my_project_1.postimage.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByImageUrlInAndUploaderId(
            List<String> urls,
            Long uploaderId
    );

    List<Image> findTop100ByImageStatusAndCreatedAtBefore(
            ImageStatus imageStatus,
            LocalDateTime time
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                DELETE FROM PostImage pi
                WHERE pi.imageStatus = :imageStatus
                  AND pi.createdAt < :time
            """)
    int deleteByImageStatusAndCreatedAtBefore(
            @Param("imageStatus") ImageStatus imageStatus,
            @Param("time") LocalDateTime time
    );
}
