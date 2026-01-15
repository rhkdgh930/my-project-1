package com.example.my_project_1.auth.oauth.info;

import java.util.Map;

public interface OAuth2UserInfo {

    String getProviderId(); // 소셜 ID (sub, id 등)
    String getProvider();   // google, kakao
    String getEmail();
    String getNickname();
}
