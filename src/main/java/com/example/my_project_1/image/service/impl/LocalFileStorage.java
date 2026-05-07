package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.image.service.ImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileStorage implements ImageStorage {

    private static final Path DEFAULT_UPLOAD_ROOT = Paths.get("uploads");
    private static final String BASE_URL = "/images/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path uploadRoot;

    public LocalFileStorage() {
        this(DEFAULT_UPLOAD_ROOT);
    }

    LocalFileStorage(Path uploadRoot) {
        this.uploadRoot = uploadRoot.toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw invalidImageFile();
            }
            String ext = getExtension(originalFilename);
            String storageKey = UUID.randomUUID() + "." + ext;
            Path path = resolveStoragePath(storageKey);

            Files.createDirectories(uploadRoot);
            Files.write(path, file.getBytes());

            return storageKey;
        } catch (IOException e) {
            throw new IllegalStateException("Image upload failed", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path path = resolveStoragePath(storageKey);
            boolean deleted = Files.deleteIfExists(path);
            if (!deleted) {
                log.warn("[IMAGE][DELETE_MISSING] key={}", storageKey);
            }
        } catch (IOException e) {
            log.warn("[IMAGE][DELETE_FAIL] key={}", storageKey, e);
            throw new IllegalStateException("Image delete failed", e);
        }
    }

    @Override
    public String getUrl(String storageKey) {
        return BASE_URL + storageKey;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw invalidImageFile();
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw invalidImageFile();
        }
        return extension;
    }

    private Path resolveStoragePath(String storageKey) {
        Path path = uploadRoot.resolve(storageKey).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw invalidImageFile();
        }
        return path;
    }

    private CustomException invalidImageFile() {
        return new CustomException(ErrorCode.INVALID_IMAGE_FILE);
    }
}
