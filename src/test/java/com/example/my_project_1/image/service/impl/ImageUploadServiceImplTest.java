package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageUploadServiceImplTest {

    private final ImageRepository imageRepository = mock(ImageRepository.class);
    private final ImageStorage imageStorage = mock(ImageStorage.class);
    private final ImageUploadServiceImpl imageUploadService =
            new ImageUploadServiceImpl(imageRepository, imageStorage);

    @Test
    @DisplayName("upload는 DB 저장 실패 시 저장된 파일을 삭제한다.")
    void upload_deletesStoredFileWhenDbSaveFails() {
        MockMultipartFile file = imageFile("image.png", "image/png", pngBytes());
        when(imageStorage.upload(file)).thenReturn("storage-key.png");
        RuntimeException dbException = new RuntimeException("db fail");
        when(imageRepository.saveAndFlush(ArgumentMatchers.any(Image.class)))
                .thenThrow(dbException);

        assertThatThrownBy(() -> imageUploadService.upload(file, 1L))
                .isSameAs(dbException);

        verify(imageStorage).delete("storage-key.png");
    }

    @Test
    @DisplayName("upload는 보상 삭제 실패 시 원래 예외를 유지한다.")
    void upload_keepsOriginalExceptionWhenCompensationDeleteFails() {
        MockMultipartFile file = imageFile("image.png", "image/png", pngBytes());
        when(imageStorage.upload(file)).thenReturn("storage-key.png");
        RuntimeException dbException = new RuntimeException("db fail");
        RuntimeException deleteException = new RuntimeException("delete fail");
        when(imageRepository.saveAndFlush(ArgumentMatchers.any(Image.class)))
                .thenThrow(dbException);
        org.mockito.Mockito.doThrow(deleteException)
                .when(imageStorage).delete("storage-key.png");

        assertThatThrownBy(() -> imageUploadService.upload(file, 1L))
                .isSameAs(dbException)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getSuppressed())
                        .contains(deleteException));
    }

    @Test
    @DisplayName("upload는 허용된 이미지 signature를 통과시킨다.")
    void upload_acceptsAllowedImageSignatures() {
        List<MockMultipartFile> files = List.of(
                imageFile("image.jpg", "image/jpeg", jpegBytes()),
                imageFile("image.jpeg", "image/jpeg", jpegBytes()),
                imageFile("image.png", "image/png", pngBytes()),
                imageFile("image.gif", "image/gif", gifBytes()),
                imageFile("image.webp", "image/webp", webpBytes())
        );

        for (MockMultipartFile file : files) {
            when(imageStorage.upload(file)).thenReturn(file.getOriginalFilename());

            imageUploadService.upload(file, 1L);

            verify(imageStorage).upload(file);
        }
    }

    @Test
    @DisplayName("upload는 svg content type을 거부한다.")
    void upload_rejectsSvgContentType() {
        MockMultipartFile file = imageFile("image.svg", "image/svg+xml", "<svg/>".getBytes());

        assertInvalidImageFile(file);

        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("upload는 확장자와 맞지 않는 content type을 거부한다.")
    void upload_rejectsMismatchedContentType() {
        MockMultipartFile file = imageFile("image.png", "text/plain", pngBytes());

        assertInvalidImageFile(file);

        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("upload는 지원하지 않는 확장자를 거부한다.")
    void upload_rejectsUnsupportedExtension() {
        MockMultipartFile file = imageFile("image.txt", "image/png", pngBytes());

        assertInvalidImageFile(file);

        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("upload는 유효하지 않은 magic byte를 거부한다.")
    void upload_rejectsInvalidMagicBytes() {
        MockMultipartFile file = imageFile("image.png", "image/png", "not-png".getBytes());

        assertInvalidImageFile(file);

        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("upload는 모든 허용 타입에서 유효하지 않은 magic byte를 거부한다.")
    void upload_rejectsInvalidMagicBytesForEveryAllowedType() {
        List<MockMultipartFile> files = List.of(
                imageFile("image.jpg", "image/jpeg", "not-jpeg".getBytes()),
                imageFile("image.jpeg", "image/jpeg", "not-jpeg".getBytes()),
                imageFile("image.png", "image/png", "not-png".getBytes()),
                imageFile("image.gif", "image/gif", "not-gif".getBytes()),
                imageFile("image.webp", "image/webp", "not-webp".getBytes())
        );

        for (MockMultipartFile file : files) {
            assertInvalidImageFile(file);
        }

        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("upload는 5MB를 초과한 파일을 거부한다.")
    void upload_rejectsFilesLargerThan5Mb() {
        MockMultipartFile file = imageFile("image.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertInvalidImageFile(file);

        verifyNoInteractions(imageStorage);
    }

    private void assertInvalidImageFile(MockMultipartFile file) {
        assertThatThrownBy(() -> imageUploadService.upload(file, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_IMAGE_FILE));
    }

    private MockMultipartFile imageFile(String originalFilename, String contentType, byte[] bytes) {
        return new MockMultipartFile(
                "file",
                originalFilename,
                contentType,
                bytes
        );
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private byte[] jpegBytes() {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        };
    }

    private byte[] gifBytes() {
        return "GIF89a".getBytes();
    }

    private byte[] webpBytes() {
        return new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        };
    }
}
