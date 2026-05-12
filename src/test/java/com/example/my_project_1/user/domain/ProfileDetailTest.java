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

    private static final String UPDATE_INTRODUCE = "?섏젙???먭린?뚭컻瑜??낅젰?댁＜?몄슂.";
    private static final String VALID_PROFILE_IMAGE_URL = "/images/550e8400-e29b-41d4-a716-446655440000.png";

    @Test
    @DisplayName("?꾨줈???앹꽦 ???꾨줈???대?吏 URL??null?대떎.")
    void profile_detail_create_success_test() {
        ProfileDetail profile = ProfileDetail.defaultProfile();

        assertThat(profile.getProfileImageUrl()).isNull();
        assertThat(profile.getIntroduce()).isNotBlank();
    }

    @Test
    @DisplayName("?꾨줈???섏젙 ??/images/{uuid}.png ?뺥깭???꾨줈???대?吏瑜??덉슜?쒕떎.")
    void profile_detail_update_success_test() {
        ProfileDetail profile = ProfileDetail.defaultProfile();

        ProfileDetail updated = ProfileDetail.update(profile, UPDATE_INTRODUCE, VALID_PROFILE_IMAGE_URL);

        assertThat(updated.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
        assertThat(updated.getIntroduce()).isEqualTo(UPDATE_INTRODUCE);
        assertThat(profile).isNotSameAs(updated);
    }

    @Test
    @DisplayName("?꾨줈???대?吏 URL??null?대㈃ 湲곗〈 媛믪쓣 ?좎??쒕떎.")
    void updateProfileImageUrl_keepsCurrentWhenNull() {
        ProfileDetail profile = ProfileDetail.update(ProfileDetail.defaultProfile(), null, VALID_PROFILE_IMAGE_URL);

        ProfileDetail updated = ProfileDetail.update(profile, UPDATE_INTRODUCE, null);

        assertThat(updated.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("?꾨줈???대?吏 URL??鍮??먯뿴?대㈃ 湲곗〈 媛믪쓣 ?좎??쒕떎.")
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
    @DisplayName("?꾨줈???대?吏 URL? ?덉슜???대?吏 ?뺤옣?먮? ?덉슜?쒕떎.")
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
    @DisplayName("?꾨줈???대?吏 URL? ?대? /images/{uuid}.{ext} ?뺥깭媛 ?꾨땲硫?嫄곕??쒕떎.")
    void updateProfileImageUrl_rejectsInvalidUrls(String profileImageUrl) {
        assertThatThrownBy(() -> ProfileDetail.update(ProfileDetail.defaultProfile(), null, profileImageUrl))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
