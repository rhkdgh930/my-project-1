package com.example.my_project_1.auth.oauth;

import com.example.my_project_1.auth.oauth.info.OAuth2UserInfo;
import com.example.my_project_1.auth.oauth.info.impl.GoogleOAuth2UserInfo;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.domain.*;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final Clock clock;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = getSocialType(registrationId);

        OAuth2UserInfo userInfo = getOAuth2UserInfo(socialType, oAuth2User.getAttributes());

        LocalDateTime now = LocalDateTime.now(clock);

        User user = saveOrUpdate(userInfo, socialType, now);

        UserWithdrawal withdrawal = user.getWithdrawal();

        LocalDateTime scheduledDeletionAt = null;
        Long remainingDays = null;
        boolean canRestore = false;

        if (withdrawal != null) {
            scheduledDeletionAt = withdrawal.getScheduledDeletionAt();
            remainingDays = withdrawal.getRemainingDays(now);
            canRestore = withdrawal.canRestore(now);
        }

        UserSuspension suspension = user.getSuspension();

        SuspensionReason suspensionReason = null;
        LocalDateTime suspendedUntil = null;

        if (suspension != null) {
            suspensionReason = suspension.getReason();
            suspendedUntil = suspension.getSuspendedUntil();
        }


        return new UserDetailsImpl(
                user.getId(),
                user.getEmail().getValue(),
                user.getPassword(),
                user.getRole().name(),
                user.getAccountStatus(),
                user.getUserStatus(),
                suspensionReason,
                suspendedUntil,
                scheduledDeletionAt,
                remainingDays,
                canRestore,
                user.isDeleted(),
                oAuth2User.getAttributes()
        );
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo, SocialType socialType, LocalDateTime now) {
        return userRepository.findByEmail(Email.from(userInfo.getEmail()))
                .map(user -> {
                    user.updateLastLogin(now);
                    return user;
                })
                .orElseGet(() -> userRepository.save(User.socialSignUp(
                        Email.from(userInfo.getEmail()),
                        userInfo.getNickname(),
                        socialType,
                        userInfo.getProviderId(),
                        now
                )));
    }

    private SocialType getSocialType(String registrationId) {
        if ("google".equals(registrationId)) return SocialType.GOOGLE;
        return SocialType.NONE;
    }

    private OAuth2UserInfo getOAuth2UserInfo(SocialType socialType, Map<String, Object> attributes) {
        if (socialType == SocialType.GOOGLE) return new GoogleOAuth2UserInfo(attributes);
        throw new OAuth2AuthenticationException("Unsupported Social Type");
    }
}
