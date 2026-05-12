package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileDetail {

    private static final String DEFAULT_INTRODUCE = "자기소개를 입력해주세요.";
    private static final String DEFAULT_IMG_URL = null;
    private static final Pattern INTERNAL_IMAGE_URL_PATTERN = Pattern.compile(
            "^/images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png|gif|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    @Size(max = 500)
    private String introduce;

    @Size(max = 2048)
    private String profileImageUrl;

    public static ProfileDetail defaultProfile() {
        return ProfileDetail.builder()
                .introduce(DEFAULT_INTRODUCE)
                .profileImageUrl(DEFAULT_IMG_URL)
                .build();
    }

    public static ProfileDetail update(ProfileDetail current, String introduce, String profileImageUrl) {
        return ProfileDetail.builder()
                .introduce(introduce != null ? introduce : current.getIntroduce())
                .profileImageUrl(resolveProfileImageUrl(current, profileImageUrl))
                .build();
    }

    private static String resolveProfileImageUrl(ProfileDetail current, String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return current.getProfileImageUrl();
        }
        return validateProfileImageUrl(profileImageUrl);
    }

    private static String validateProfileImageUrl(String profileImageUrl) {
        if (!INTERNAL_IMAGE_URL_PATTERN.matcher(profileImageUrl).matches()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return profileImageUrl;
    }

    @Builder
    private ProfileDetail(String introduce, String profileImageUrl) {
        this.introduce = (introduce != null) ? introduce : DEFAULT_INTRODUCE;
        this.profileImageUrl = (profileImageUrl != null) ? validateProfileImageUrl(profileImageUrl) : DEFAULT_IMG_URL;
    }
}
