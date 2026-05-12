package com.example.my_project_1.user.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "프로필 수정 요청")
public class UserProfileUpdateRequest {
    @Schema(description = "자기소개. 최대 500자입니다.", example = "백엔드 개발자입니다.", maxLength = 500)
    @Size(max = 500)
    private String introduce;

    @Schema(description = "프로필 이미지 URL. 내부 업로드 이미지인 /images/{uuid}.{ext} 형식만 허용합니다.", example = "/images/550e8400-e29b-41d4-a716-446655440000.png")
    @Size(max = 2048)
    private String profileImageUrl;
}
