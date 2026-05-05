package com.example.my_project_1.image.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path uploadRoot;

    @Test
    @DisplayName("upload stores files with allowed image extensions")
    void upload_storesFilesWithAllowedImageExtensions() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        for (String extension : List.of("jpg", "jpeg", "png", "gif", "webp")) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image." + extension.toUpperCase(),
                    "image/" + extension,
                    pngBytes()
            );

            String storageKey = localFileStorage.upload(file);

            assertThat(storageKey).endsWith("." + extension);
            assertThat(Files.exists(uploadRoot.resolve(storageKey))).isTrue();
        }
    }

    @Test
    @DisplayName("upload rejects unsupported extensions")
    void upload_rejectsUnsupportedExtensions() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.svg",
                "image/svg+xml",
                "<svg/>".getBytes()
        );

        assertThatThrownBy(() -> localFileStorage.upload(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("upload rejects path traversal in extension")
    void upload_rejectsPathTraversalInExtension() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png/../../evil",
                "image/png",
                pngBytes()
        );

        assertThatThrownBy(() -> localFileStorage.upload(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("delete treats missing file as success")
    void delete_treatsMissingFileAsSuccess() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        String storageKey = "missing-%s.png".formatted(UUID.randomUUID());

        assertThatCode(() -> localFileStorage.delete(storageKey))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("delete propagates IOException")
    void delete_propagatesIOException() throws IOException {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        String storageKey = "delete-fail-%s".formatted(UUID.randomUUID());
        Path createdPath = uploadRoot.resolve(storageKey);
        Files.createDirectories(createdPath);
        Files.writeString(createdPath.resolve("child.txt"), "child");

        assertThatThrownBy(() -> localFileStorage.delete(storageKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("delete rejects storage keys outside upload root")
    void delete_rejectsStorageKeysOutsideUploadRoot() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);

        assertThatThrownBy(() -> localFileStorage.delete("../evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
