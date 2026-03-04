package com.example.my_project_1.user.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProfileDetailTest {
    private static final String DEFAULT_INTRODUCE = "자기소개를 입력해주세요.";
    private static final String DEFAULT_IMG_URL = "uploads/default.png";

    private static final String UPDATE_INTRODUCE = "수정된 자기소개를 입력해주세요.";
    private static final String UPDATE_IMG_URL = "new/uploads/default.png";

    @DisplayName("프로필 생성에 성공합니다.")
    @Test
    void profile_detail_create_success_test() {
        ProfileDetail profile = getProfile();
        assertThat(profile.getProfileImageUrl()).isEqualTo(DEFAULT_IMG_URL);
        assertThat(profile.getIntroduce()).isEqualTo(DEFAULT_INTRODUCE);
    }

    private static ProfileDetail getProfile() {
        return ProfileDetail.defaultProfile();
    }

    @DisplayName("프로필 수정에 성공합니다.")
    @Test
    void profile_detail_update_success_test() {
        ProfileDetail profile = getProfile();
        assertThat(profile.getProfileImageUrl()).isEqualTo(DEFAULT_IMG_URL);
        assertThat(profile.getIntroduce()).isEqualTo(DEFAULT_INTRODUCE);

        ProfileDetail updated = ProfileDetail.update(UPDATE_INTRODUCE, UPDATE_IMG_URL);
        assertThat(updated.getProfileImageUrl()).isEqualTo(UPDATE_IMG_URL);
        assertThat(updated.getIntroduce()).isEqualTo(UPDATE_INTRODUCE);

        assertThat(profile).isNotSameAs(updated);
    }

}