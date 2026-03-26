package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.service.ImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileStorage implements ImageStorage {

    private static final String UPLOAD_DIR = "uploads/";
    private static final String BASE_URL = "/images/";

    @Override
    public String upload(MultipartFile file) {
        try {
            String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
            String storageKey = UUID.randomUUID() + "." + ext;

            Path path = Paths.get(UPLOAD_DIR, storageKey);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return storageKey;
        } catch (IOException e) {
            throw new IllegalStateException("이미지 업로드 실패", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path path = Paths.get(UPLOAD_DIR, storageKey);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[IMAGE][DELETE_FAIL] key={}", storageKey, e);
        }
    }

    @Override
    public String getUrl(String storageKey) {
        return BASE_URL + storageKey;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("확장자가 없는 파일입니다.");
        }
        return filename.substring(dotIndex + 1);
    }
}
