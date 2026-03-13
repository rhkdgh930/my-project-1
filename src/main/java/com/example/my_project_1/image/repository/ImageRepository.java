package com.example.my_project_1.image.repository;

import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByStorageKeyInAndUploaderId(List<String> storageKeys, Long uploaderId);

    List<Image> findAllByOwnerIdAndOwnerType(Long ownerId, ImageOwnerType ownerType);

    @Modifying
    @Query("""
        UPDATE Image i
        SET i.ownerId = null,
            i.ownerType = null,
            i.imageStatus = 'DETACHED'
        WHERE i.ownerId = :ownerId
        AND i.ownerType = :ownerType
    """)
    void bulkDetachAll(Long ownerId, ImageOwnerType ownerType);

    List<Image> findTop100ByImageStatusAndCreatedAtBefore(
            ImageStatus status,
            LocalDateTime time
    );
}
