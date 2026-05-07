package com.example.my_project_1.image.repository;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageOwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:image-repository-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Test
    @DisplayName("detached cleanup 대상은 createdAt 대신 detachedAt을 기준으로 조회한다.")
    void findDetachedCleanupTargets_usesDetachedAtInsteadOfCreatedAt() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 10, 0);
        Image recentDetached = detachedImage(
                "recent-detached.png",
                now.minusDays(30),
                now.minusHours(1)
        );
        Image oldDetached = detachedImage(
                "old-detached.png",
                now.minusDays(30),
                now.minusDays(8)
        );
        imageRepository.saveAllAndFlush(List.of(recentDetached, oldDetached));

        List<Image> targets = imageRepository.findDetachedCleanupTargets(
                0L,
                now.minusDays(7),
                PageRequest.ofSize(10)
        );

        assertThat(targets)
                .extracting(Image::getStorageKey)
                .containsExactly("old-detached.png");
    }

    private Image detachedImage(
            String storageKey,
            LocalDateTime createdAt,
            LocalDateTime detachedAt
    ) {
        Image image = Image.createPending(1L, storageKey);
        image.attach(10L, ImageOwnerType.POST);
        image.detach(detachedAt);
        ReflectionTestUtils.setField(image, "createdAt", createdAt);
        return image;
    }
}
