package com.example.my_project_1.image.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.image.service.ImageStorage;
import com.example.my_project_1.image.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Image API", description = "이미지 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageUploadService imageUploadService;
    private final ImageStorage imageStorage;

    @Operation(
            summary = "이미지 업로드",
            description = "multipart/form-data로 이미지 파일을 업로드합니다. part name은 file입니다. 허용 확장자는 jpg, jpeg, png, gif, webp이고 허용 contentType은 image/jpeg, image/png, image/gif, image/webp입니다. SVG는 금지되며 최대 크기는 5MB입니다. 확장자/contentType/magic byte를 검증하고 저장 경로는 upload root 밖으로 벗어나지 않도록 방어합니다. 성공 응답의 url은 /images/{storageKey} 형태의 정적 이미지 URL입니다. ImageUrlParser는 Markdown 본문에서 /images/{uuid}.{ext} 내부 URL만 image lifecycle attach/sync 대상으로 인정하며 외부 URL, query, fragment, encoded path는 제외합니다.",
            security = @SecurityRequirement(name = "jwtAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schemaProperties = {
                                    @SchemaProperty(name = "file",
                                            schema = @Schema(type = "string", format = "binary",
                                                    description = "업로드할 이미지 파일"))
                            },
                            encoding = @Encoding(name = "file", contentType = "image/jpeg, image/png, image/gif, image/webp")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공",
                    content = @Content(schema = @Schema(implementation = ImageUploadResponseSchema.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 입력. INVALID_IMAGE_FILE 또는 INVALID_INPUT_VALUE 계열 오류입니다.",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "500", description = "파일 시스템 I/O 실패 또는 서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> upload(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam MultipartFile file
    ) {
        Long uploaderId = userDetails.getUserId();
        String storageKey = imageUploadService.upload(file, uploaderId);
        String url = imageStorage.getUrl(storageKey);

        return ResponseEntity.ok(Map.of(
                "storageKey", storageKey,
                "url", url
        ));
    }

    @Schema(name = "ImageUploadResponse", description = "이미지 업로드 성공 응답. 실제 API는 Map 형태로 storageKey와 url을 반환합니다.")
    private record ImageUploadResponseSchema(
            @Schema(description = "서버가 생성한 저장 키", example = "550e8400-e29b-41d4-a716-446655440000.png")
            String storageKey,

            @Schema(description = "정적 이미지 조회 URL", example = "/images/550e8400-e29b-41d4-a716-446655440000.png")
            String url
    ) {
    }
}
