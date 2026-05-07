package com.example.my_project_1.auth.oauth;

import com.example.my_project_1.auth.oauth.info.OAuth2UserInfo;
import com.example.my_project_1.auth.oauth.info.impl.GoogleOAuth2UserInfo;
import com.example.my_project_1.auth.service.UserAccountPolicy;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.SocialType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final Clock clock;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountPolicy userAccountPolicy;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = getSocialType(registrationId);

        OAuth2UserInfo userInfo = getOAuth2UserInfo(socialType, oAuth2User.getAttributes());
        validateOAuth2UserInfo(userInfo);

        LocalDateTime now = LocalDateTime.now(clock);

        User user = saveOrGetSocialUser(userInfo, socialType, now);

        user.checkAndReleaseSuspension(now);
        userAccountPolicy.validateLoginAllowed(user, now);

        return UserDetailsImpl.from(user, now, oAuth2User.getAttributes());
    }

    private User saveOrGetSocialUser(
            OAuth2UserInfo userInfo,
            SocialType socialType,
            LocalDateTime now
    ) {
        Email email = Email.from(userInfo.getEmail());

        return userRepository.findByEmail(email)
                .map(user -> validateExistingSocialAccount(user, socialType, userInfo))
                .orElseGet(() -> createSocialUser(userInfo, socialType, now));
    }

    private User validateExistingSocialAccount(
            User user,
            SocialType socialType,
            OAuth2UserInfo userInfo
    ) {
        if (user.getSocialType() != socialType) {
            throw new OAuth2AuthenticationException("Already registered with different login method");
        }

        if (!userInfo.getProviderId().equals(user.getSocialId())) {
            throw new OAuth2AuthenticationException("OAuth2 account does not match");
        }

        return user;
    }

    private User createSocialUser(
            OAuth2UserInfo userInfo,
            SocialType socialType,
            LocalDateTime now
    ) {
        String encodedRandomPassword =
                passwordEncoder.encode(UUID.randomUUID().toString());

        return userRepository.save(User.socialSignUp(
                Email.from(userInfo.getEmail()),
                encodedRandomPassword,
                userInfo.getNickname(),
                socialType,
                userInfo.getProviderId(),
                now
        ));
    }

    private void validateOAuth2UserInfo(OAuth2UserInfo userInfo) {
        if (!StringUtils.hasText(userInfo.getProviderId())) {
            throw new OAuth2AuthenticationException("OAuth2 provider id is required");
        }

        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException("OAuth2 email is required");
        }

        if (!userInfo.isEmailVerified()) {
            throw new OAuth2AuthenticationException("OAuth2 email is not verified");
        }
    }

    private SocialType getSocialType(String registrationId) {
        if ("google".equals(registrationId)) {
            return SocialType.GOOGLE;
        }

        throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
    }

    private OAuth2UserInfo getOAuth2UserInfo(
            SocialType socialType,
            Map<String, Object> attributes
    ) {
        if (socialType == SocialType.GOOGLE) {
            return new GoogleOAuth2UserInfo(attributes);
        }

        throw new OAuth2AuthenticationException("Unsupported Social Type");
    }
}