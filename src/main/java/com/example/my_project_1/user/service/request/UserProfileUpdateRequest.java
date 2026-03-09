package com.example.my_project_1.user.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;

@Getter
public class UserProfileUpdateRequest {
    @Schema(title = "자기 소개", description = "최대 500자 이내의 소개글", example = "백엔드 개발자 지망생입니다.")
    @Size(max = 500)
    private String introduce;

    @Schema(title = "프로필 이미지 URL", description = "이미지 업로드 API를 통해 얻은 URL 주소",
            example = "https://cdn.example.com/profiles/user1.png")
    @URL
    private String profileImageUrl;
}
