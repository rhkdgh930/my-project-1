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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [Policy] 이 엔티티는 Soft Delete 정책을 따릅니다.
 * 삭제 시 실제 DELETE 대신 @SQLDelete에 정의된 UPDATE가 실행되며,
 *
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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus; //ACTIVE, WITHDRAWN(탈퇴), WITHDRAWN_REQUESTED(탈퇴 요청), DORMANT(휴면)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus; //NORMAL, SUSPENDED

    @Enumerated(EnumType.STRING)
    private SocialType socialType; //NONE, GOOGLE

    private String socialId;

    @Column(nullable = false)
    private boolean isEmailVerified;

    private LocalDateTime lastLoginAt;

    public static User signUp(Email email, String encodedPassword, String nickname, LocalDateTime now) {
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
                .lastLoginAt(now)
                .build();
    }

    public static User socialSignUp(Email email, String nickname, SocialType socialType, String socialId, LocalDateTime now) {
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
                .lastLoginAt(now)
                .build();
    }

    // =========================================================
    // [Group A] 핵심 서비스 액션 (Full Access)
    // =========================================================

    public void updateProfile(String introduce, String profileImageUrl) {
        requireFullAccess();
        this.profileDetail = ProfileDetail.update(introduce, profileImageUrl);
    }

    public void updateNickname(String nickname) {
        requireFullAccess();
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        this.nickname = nickname;
    }

    public void markDormant() {
        requireFullAccess();
        this.userStatus = UserStatus.DORMANT;
    }

    // =========================================================
    // [Group B] 계정 관리 액션 (Account Manageable)
    // =========================================================

    public void updatePassword(String encodedPassword) {
        requireAccountManageable();
        Assert.hasText(encodedPassword, "비밀번호는 필수입니다.");
        this.password = encodedPassword;
    }

    public void requestWithdrawal(LocalDateTime now) {
        requireAccountManageable();
        Assert.notNull(now, "시간은 필수입니다.");
        this.userStatus = UserStatus.WITHDRAWN_REQUESTED;
        this.withdrawal = UserWithdrawal.request(now);
    }

    public void updateLastLogin(LocalDateTime now) {
        requireAccountManageable();
        Assert.notNull(now, "시간은 필수입니다.");
        if (this.lastLoginAt != null && this.lastLoginAt.toLocalDate().isEqual(now.toLocalDate())) return;
        this.lastLoginAt = now;
    }

    // =========================================================
    // [Group C] 특수 상태 전환 액션 (System / Admin / Lifecycle)
    // =========================================================

    public void activateFromDormant(LocalDateTime now) {
        if (this.userStatus != UserStatus.DORMANT) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        }
        this.userStatus = UserStatus.ACTIVE;
        this.updateLastLogin(now);
    }

    public void cancelWithdrawal(LocalDateTime now) {
        if (this.userStatus != UserStatus.WITHDRAWN_REQUESTED) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        }
        if (this.withdrawal != null && this.withdrawal.canRestore(now)) {
            this.userStatus = UserStatus.ACTIVE;
            this.withdrawal = null;
        }
    }

    public void completeWithdrawal() {
        if (this.userStatus != UserStatus.WITHDRAWN_REQUESTED) throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        maskPersonalData();
        this.userStatus = UserStatus.WITHDRAWN;
    }

    public void checkAndReleaseSuspension(LocalDateTime now) {
        if (this.accountStatus == AccountStatus.SUSPENDED && this.suspension != null && !this.suspension.isActive(now)) {
            this.accountStatus = AccountStatus.NORMAL;
            this.suspension = null;
        }
    }

    public void suspend(SuspensionType type, SuspensionReason reason, Duration duration, LocalDateTime now) {
        requireAccountPresent();

        if (this.accountStatus != AccountStatus.SUSPENDED || this.suspension == null) {
            this.accountStatus = AccountStatus.SUSPENDED;
            this.suspension = UserSuspension.create(type, reason, now, duration);
        } else {
            this.suspension = this.suspension.mergeWith(type, reason, now, duration);
        }
    }

    // =========================================================
    // [Validation] 계층적 자격 검증 로직 (Hierarchical Eligibility)
    // =========================================================

    /**
     * 1단계: 계정이 데이터베이스 상에 유효하게 존재하는가?
     */
    private void requireAccountPresent() {
        if (super.isDeleted() || this.userStatus == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 2단계: 계정을 관리(수정)할 수 있는 상태인가? (휴면 포함)
     */
    private void requireAccountManageable() {
        requireAccountPresent(); // 1단계 검증 포함
        if (this.userStatus == UserStatus.WITHDRAWN_REQUESTED) {
            throw new CustomException(ErrorCode.INVALID_USER_STATUS);
        }
    }

    /**
     * 3단계: 서비스를 온전히 이용할 수 있는 상태인가?
     */
    private void requireFullAccess() {
        requireAccountManageable();

        if (this.userStatus == UserStatus.DORMANT) {
            throw new CustomException(ErrorCode.USER_DORMANT);
        }
        if (this.userStatus != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }
        if (this.accountStatus == AccountStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
    }

    public boolean isActive() {
        return !super.isDeleted() && accountStatus == AccountStatus.NORMAL && userStatus == UserStatus.ACTIVE;
    }

    public boolean isSuspended(LocalDateTime now) {
        return this.accountStatus == AccountStatus.SUSPENDED
                && this.suspension != null
                && this.suspension.isActive(now);
    }

    public boolean isWithdrawnCompletely() {
        return this.userStatus == UserStatus.WITHDRAWN;
    }

    // =========================================================

    private void maskPersonalData() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        this.email = Email.from("deleted_" + uuid + "_" + this.email.getValue());
        this.nickname = "알수없음_" + uuid;
        this.password = "WITHDRAWN";
    }

    @Builder
    private User(Email email, ProfileDetail profileDetail, String password, String nickname, Role role,
                 UserStatus userStatus, AccountStatus accountStatus, SocialType socialType, String socialId,
                 boolean isEmailVerified, LocalDateTime lastLoginAt) {

        Assert.notNull(email, "이메일은 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        Assert.notNull(lastLoginAt, "초기 로그인 시간은 필수입니다.");

        this.email = email;
        this.profileDetail = profileDetail;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.userStatus = userStatus;
        this.accountStatus = accountStatus;
        this.socialType = socialType;
        this.socialId = socialId;
        this.isEmailVerified = isEmailVerified;
        this.lastLoginAt = lastLoginAt;
    }
}
