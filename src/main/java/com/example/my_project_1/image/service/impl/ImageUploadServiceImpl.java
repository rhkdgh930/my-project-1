package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import com.example.my_project_1.image.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {
    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;

    @Override
    public String upload(MultipartFile file, Long uploaderId) {
        validate(file);

        String storageKey = imageStorage.upload(file);
        Image image = Image.createPending(uploaderId, storageKey);
        imageRepository.save(image);

        return storageKey;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기 초과");
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 가능");
        }

    }
}
