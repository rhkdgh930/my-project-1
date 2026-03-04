package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileDetail {

    private static final String DEFAULT_INTRODUCE = "자기소개를 입력해주세요.";
    private static final String DEFAULT_IMG_URL = "uploads/default.png";

    private String introduce;
    private String profileImageUrl;

    public static ProfileDetail defaultProfile() {
        return ProfileDetail.builder()
                .introduce(DEFAULT_INTRODUCE)
                .profileImageUrl(DEFAULT_IMG_URL)
                .build();
    }

    public static ProfileDetail update(String introduce, String profileImageUrl) {
        return ProfileDetail.builder()
                .introduce(introduce)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    @Builder
    private ProfileDetail(String introduce, String profileImageUrl) {
        this.introduce = (introduce != null) ? introduce : DEFAULT_INTRODUCE;
        this.profileImageUrl = (profileImageUrl != null) ? profileImageUrl : DEFAULT_IMG_URL;
    }
}
