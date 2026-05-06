package com.example.my_project_1.user.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;

@Getter
@Schema(description = "프로필 수정 요청")
public class UserProfileUpdateRequest {
    @Schema(description = "자기소개. 최대 500자입니다.", example = "백엔드 개발자입니다.", maxLength = 500)
    @Size(max = 500)
    private String introduce;

    @Schema(description = "프로필 이미지 URL", example = "http://localhost:8080/images/default.png")
    @URL
    private String profileImageUrl;
}
