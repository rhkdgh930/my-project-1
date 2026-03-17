package com.example.my_project_1.image.batch;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageBatchProcessor {

    private final ImageRepository imageRepository;

    @Transactional
    public void markDeletedBulk(List<Long> ids) {
        if (ids.isEmpty()) return;
        imageRepository.bulkMarkDeleted(ids);
    }
}