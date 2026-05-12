package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileDetail {

    private static final String DEFAULT_INTRODUCE = "자기소개를 입력해주세요.";
    private static final String DEFAULT_IMG_URL = "http://localhost:8080/images/default.png";

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
                .profileImageUrl(profileImageUrl != null ? profileImageUrl : current.getProfileImageUrl())
                .build();
    }

    @Builder
    private ProfileDetail(String introduce, String profileImageUrl) {
        this.introduce = (introduce != null) ? introduce : DEFAULT_INTRODUCE;
        this.profileImageUrl = (profileImageUrl != null) ? profileImageUrl : DEFAULT_IMG_URL;
    }
}
