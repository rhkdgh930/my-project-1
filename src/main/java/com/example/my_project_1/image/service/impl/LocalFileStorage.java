package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.service.ImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStorage implements ImageStorage {

    private static final String UPLOAD_DIR = "uploads/";

    @Override
    public String upload(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = getExtension(originalFilename);

            String filename = UUID.randomUUID() + "." + ext;
            Path path = Paths.get(UPLOAD_DIR, filename);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return "/images/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("이미지 업로드 실패", e);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("확장자가 없는 파일입니다.");
        }
        return filename.substring(dotIndex + 1);
    }

    @Override
    public void delete(String imageUrl) {
        try {
            String filename = imageUrl.replace("/images/", "");
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("이미지 삭제 실패", e);
        }
    }
}
