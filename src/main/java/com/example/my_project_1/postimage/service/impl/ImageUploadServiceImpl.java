package com.example.my_project_1.postimage.service.impl;

import com.example.my_project_1.postimage.domain.Image;
import com.example.my_project_1.postimage.repository.PostImageRepository;
import com.example.my_project_1.postimage.service.ImageStorage;
import com.example.my_project_1.postimage.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {
    private final PostImageRepository postImageRepository;
    private final ImageStorage imageStorage;

    @Override
    public String upload(MultipartFile file, Long uploaderId) {
        validate(file);

        // DB에 먼저 pending상태로 저장 후, url받아오면 상태 변경
        Image image = postImageRepository.save(
                Image.pending(uploaderId)
        );

        String imageUrl = imageStorage.upload(file);

        image.uploaded(imageUrl);

        return imageUrl;
    }

    private void validate(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 허용");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기 초과");
        }
    }
}
