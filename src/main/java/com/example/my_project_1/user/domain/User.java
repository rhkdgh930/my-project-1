package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [Policy] 이 엔티티는 Soft Delete 정책을 따릅니다.
 * 삭제 시 실제 DELETE 대신 @SQLDelete에 정의된 UPDATE가 실행되며,
 * @SQLRestriction에 의해 삭제되지 않은 데이터만 기본 조회됩니다.
 */

@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @Column(nullable = false)
    private Email email;

    @Embedded
    private ProfileDetail profileDetail;

    @Embedded
    private UserSuspension suspension;

    @Embedded
    private UserWithdrawal withdrawal;

    @Embedded
    private UserDormancy dormancy;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus; //ACTIVE, WITHDRAWN(탈퇴), DORMANT(휴면)


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus; //NORMAL, SUSPENDED

    @Enumerated(EnumType.STRING)
    private SocialType socialType; //NONE, GOOGLE

    private String socialId;

    @Column(nullable = false)
    private boolean isEmailVerified;

    private LocalDateTime lastLoginAt;

    public static User signUp(Email email, String encodedPassword, String nickname) {
        return User.builder()
                .email(email)
                .profileDetail(ProfileDetail.defaultProfile())
                .password(encodedPassword)
                .nickname(nickname)
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .accountStatus(AccountStatus.NORMAL)
                .isEmailVerified(true)
                .socialType(SocialType.NONE)
                .build();
    }

    public static User socialSignUp(Email email, String nickname, SocialType socialType, String socialId) {
        String randomPassword = UUID.randomUUID().toString();
        return User.builder()
                .email(email)
                .profileDetail(ProfileDetail.defaultProfile())
                .password(randomPassword)
                .nickname(nickname)
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .accountStatus(AccountStatus.NORMAL)
                .isEmailVerified(true)
                .socialType(socialType)
                .socialId(socialId)
                .build();
    }

    public void suspend(SuspensionType type, SuspensionReason reason, Duration duration, LocalDateTime now) {
        validateSuspendable();

        if (this.accountStatus != AccountStatus.SUSPENDED || this.suspension == null) {
            this.accountStatus = AccountStatus.SUSPENDED;
            this.suspension = UserSuspension.create(type, reason, now, duration);
        } else {
            this.suspension = this.suspension.mergeWith(type, reason, now, duration);
        }
    }

    private void validateSuspendable() {
        if (super.isDeleted() || userStatus == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    public void completeWithdrawal() {
        validateWithdrawalPending();
        maskPersonalData();
        this.userStatus = UserStatus.WITHDRAWN;
    }

    private void maskPersonalData() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        this.email = Email.from("deleted_" + uuid + "_" + this.email.getValue());
        this.nickname = "알수없음_" + uuid;
        this.password = "WITHDRAWN";
    }

    public void cancelWithdrawal(LocalDateTime now) {
        validateWithdrawalPending();
        if (this.withdrawal.isPending() && this.withdrawal.canRestore(now)) {
            this.userStatus = UserStatus.ACTIVE;
            this.withdrawal = null;
        }
    }

    private void validateWithdrawalPending() {
        if (this.userStatus != UserStatus.WITHDRAWN_REQUESTED) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        }
    }

    public void requestWithdrawal() {
        validateActive();
        this.userStatus = UserStatus.WITHDRAWN_REQUESTED;
        this.withdrawal = UserWithdrawal.request(LocalDateTime.now());
    }

    public void updatePassword(String encodedPassword) {
        validateActive();
        Assert.hasText(encodedPassword, "비밀번호는 필수입니다.");
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        validateActive();
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        this.nickname = nickname;
    }

    public void updateProfile(String introduce, String profileImageUrl) {
        validateActive();
        this.profileDetail = ProfileDetail.update(introduce, profileImageUrl);
    }

    public void markDormant() {
        validateActive();
        this.userStatus = UserStatus.DORMANT;
    }

    private void validateActive() {
        if (!isActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }
    }

    public boolean isActive() {
        return !super.isDeleted() && accountStatus == AccountStatus.NORMAL && userStatus == UserStatus.ACTIVE;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    @Builder
    private User(Email email, ProfileDetail profileDetail, String password, String nickname, Role role,
                 UserStatus userStatus, AccountStatus accountStatus, SocialType socialType, String socialId,
                 boolean isEmailVerified) {

        Assert.notNull(email, "이메일은 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");

        this.email = email;
        this.profileDetail = profileDetail;
        this.password = password;
        this.nickname = nickname;
        this.role = (role != null) ? role : Role.USER;
        this.userStatus = (userStatus != null) ? userStatus : UserStatus.ACTIVE;
        this.accountStatus = (accountStatus != null) ? accountStatus : AccountStatus.NORMAL;
        this.socialType = (socialType != null) ? socialType : SocialType.NONE;
        this.socialId = socialId;
        this.isEmailVerified = isEmailVerified;
    }
}
