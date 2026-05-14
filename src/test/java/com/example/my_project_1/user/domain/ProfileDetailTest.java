package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileDetailTest {

    private static final String UPDATE_INTRODUCE = "수정된 자기소개를 입력합니다.";
    private static final String VALID_PROFILE_IMAGE_URL = "/images/550e8400-e29b-41d4-a716-446655440000.png";

    @Test
    @DisplayName("프로필 생성 시 프로필 이미지 URL은 null이다.")
    void profile_detail_create_success_test() {
        ProfileDetail profile = ProfileDetail.defaultProfile();

        assertThat(profile.getProfileImageUrl()).isNull();
        assertThat(profile.getIntroduce()).isNotBlank();
    }

    @Test
    @DisplayName("프로필 수정 시 /images/{uuid}.png 형식의 프로필 이미지를 허용한다.")
    void profile_detail_update_success_test() {
        ProfileDetail profile = ProfileDetail.defaultProfile();

        ProfileDetail updated = ProfileDetail.update(profile, UPDATE_INTRODUCE, VALID_PROFILE_IMAGE_URL);

        assertThat(updated.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
        assertThat(updated.getIntroduce()).isEqualTo(UPDATE_INTRODUCE);
        assertThat(profile).isNotSameAs(updated);
    }

    @Test
    @DisplayName("프로필 이미지 URL이 null이면 기존 값을 유지한다.")
    void updateProfileImageUrl_keepsCurrentWhenNull() {
        ProfileDetail profile = ProfileDetail.update(ProfileDetail.defaultProfile(), null, VALID_PROFILE_IMAGE_URL);

        ProfileDetail updated = ProfileDetail.update(profile, UPDATE_INTRODUCE, null);

        assertThat(updated.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("프로필 이미지 URL이 빈 문자열이면 기존 값을 유지한다.")
    void updateProfileImageUrl_keepsCurrentWhenBlank() {
        ProfileDetail profile = ProfileDetail.update(ProfileDetail.defaultProfile(), null, VALID_PROFILE_IMAGE_URL);

        ProfileDetail updated = ProfileDetail.update(profile, UPDATE_INTRODUCE, " ");

        assertThat(updated.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/images/550e8400-e29b-41d4-a716-446655440000.jpg",
            "/images/550e8400-e29b-41d4-a716-446655440000.jpeg",
            "/images/550e8400-e29b-41d4-a716-446655440000.png",
            "/images/550e8400-e29b-41d4-a716-446655440000.gif",
            "/images/550e8400-e29b-41d4-a716-446655440000.webp"
    })
    @DisplayName("프로필 이미지 URL은 허용된 이미지 확장자만 허용한다.")
    void updateProfileImageUrl_allowsSupportedExtensions(String profileImageUrl) {
        ProfileDetail updated = ProfileDetail.update(ProfileDetail.defaultProfile(), null, profileImageUrl);

        assertThat(updated.getProfileImageUrl()).isEqualTo(profileImageUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/images/not-uuid.png",
            "https://example.com/a.png",
            "javascript:alert(1)",
            "data:image/png;base64,abc",
            "//evil.com/a.png",
            "/images/550e8400-e29b-41d4-a716-446655440000.svg",
            "/images/550e8400-e29b-41d4-a716-446655440000.png?x=1",
            "/images/550e8400-e29b-41d4-a716-446655440000.png#x",
            "../evil.png"
    })
    @DisplayName("프로필 이미지 URL은 내부 /images/{uuid}.{ext} 형식이 아니면 거부한다.")
    void updateProfileImageUrl_rejectsInvalidUrls(String profileImageUrl) {
        assertThatThrownBy(() -> ProfileDetail.update(ProfileDetail.defaultProfile(), null, profileImageUrl))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
