package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import com.example.my_project_1.image.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES_BY_EXTENSION = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif"),
            "webp", Set.of("image/webp")
    );

    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;

    @Override
    public String upload(MultipartFile file, Long uploaderId) {
        validate(file);

        String storageKey = imageStorage.upload(file);

        try {
            imageRepository.saveAndFlush(
                    Image.createPending(uploaderId, storageKey)
            );
        } catch (RuntimeException e) {
            deleteUploadedFileQuietly(storageKey, e);
            throw e;
        }
        return storageKey;
    }

    private void deleteUploadedFileQuietly(String storageKey, RuntimeException originalException) {
        try {
            imageStorage.delete(storageKey);
        } catch (RuntimeException deleteException) {
            log.warn("[IMAGE][UPLOAD_COMPENSATION_DELETE_FAIL] key={}", storageKey, deleteException);
            originalException.addSuppressed(deleteException);
        }
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image file size exceeded");
        }

        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (contentType == null ||
                !ALLOWED_CONTENT_TYPES_BY_EXTENSION.get(extension).contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported image content type");
        }

        validateMagicBytes(file, extension);
    }

    private String getExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Image extension is required");
        }

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Image extension is required");
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES_BY_EXTENSION.containsKey(extension)) {
            throw new IllegalArgumentException("Unsupported image extension");
        }
        return extension;
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            if (!hasValidSignature(bytes, extension)) {
                throw new IllegalArgumentException("Invalid image file signature");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read image file", e);
        }
    }

    private boolean hasValidSignature(byte[] bytes, String extension) {
        return switch (extension) {
            case "png" -> isPng(bytes);
            case "jpg", "jpeg" -> isJpeg(bytes);
            case "gif" -> isGif(bytes);
            case "webp" -> isWebp(bytes);
            default -> false;
        };
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 0x47
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x38
                && (bytes[4] == 0x37 || bytes[4] == 0x39)
                && bytes[5] == 0x61;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50;
    }
}
