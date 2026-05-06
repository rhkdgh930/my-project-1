package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
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
    @DisplayName("upload는 허용된 이미지 확장자의 파일을 저장한다.")
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
    @DisplayName("upload는 지원하지 않는 확장자를 거부한다.")
    void upload_rejectsUnsupportedExtensions() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.svg",
                "image/svg+xml",
                "<svg/>".getBytes()
        );

        assertInvalidImageFile(() -> localFileStorage.upload(file));
    }

    @Test
    @DisplayName("upload는 확장자의 path traversal 시도를 거부한다.")
    void upload_rejectsPathTraversalInExtension() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png/../../evil",
                "image/png",
                pngBytes()
        );

        assertInvalidImageFile(() -> localFileStorage.upload(file));
    }

    @Test
    @DisplayName("delete는 파일이 없어도 성공으로 처리한다.")
    void delete_treatsMissingFileAsSuccess() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);
        String storageKey = "missing-%s.png".formatted(UUID.randomUUID());

        assertThatCode(() -> localFileStorage.delete(storageKey))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("delete는 IOException을 전파한다.")
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
    @DisplayName("delete는 upload root 밖의 storageKey를 거부한다.")
    void delete_rejectsStorageKeysOutsideUploadRoot() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot);

        assertInvalidImageFile(() -> localFileStorage.delete("../evil.png"));
    }

    private void assertInvalidImageFile(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_IMAGE_FILE));
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
