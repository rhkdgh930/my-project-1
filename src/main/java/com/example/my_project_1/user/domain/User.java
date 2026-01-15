package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Embedded
    private ProfileDetail profileDetail;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus; //PENDING, ACTIVE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus; //NORMAL, SUSPENDED

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean deleted;

    private LocalDateTime lastLoginAt;

    public static User signUp(Email email, ProfileDetail profileDetail, String encodedPassword, String nickname) {
        return User.builder()
                .email(email)
                .profileDetail(profileDetail)
                .password(encodedPassword)
                .nickname(nickname)
                .role(Role.USER)
                .userStatus(UserStatus.PENDING)
                .accountStatus(AccountStatus.NORMAL)
                .emailVerified(false)
                .build();
    }

    public void suspend() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        if (isSuspended()) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        this.accountStatus = AccountStatus.SUSPENDED;
    }

    public void delete() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        this.deleted = true;
    }

    public void withdraw() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        this.deleted = true;
    }

    public void verifyEmail() {
        if (this.userStatus != UserStatus.PENDING) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED_USER);
        }
        this.emailVerified = true;
        this.userStatus = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return !deleted && accountStatus == AccountStatus.NORMAL && userStatus == UserStatus.ACTIVE;
    }

    public void updatePassword(String encodedPassword) {
        checkActiveUser();
        Assert.hasText(encodedPassword, "비밀번호는 필수입니다.");
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        checkActiveUser();
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        this.nickname = nickname;
    }

    public void updateProfile(String introduce, String profileImageUrl) {
        checkActiveUser();
        this.profileDetail = ProfileDetail.update(introduce, profileImageUrl);
    }

    private void checkActiveUser() {
        if (!isActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isSuspended() {
        return accountStatus == AccountStatus.SUSPENDED;
    }

    @Builder
    private User(Email email, ProfileDetail profileDetail, String password, String nickname, Role role,
                 UserStatus userStatus, AccountStatus accountStatus,
                 boolean emailVerified) {

        Assert.notNull(email, "이메일은 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");

        this.email = email;
        this.profileDetail = profileDetail;
        this.password = password;
        this.nickname = nickname;
        this.role = (role != null) ? role : Role.USER;
        this.userStatus = (userStatus != null) ? userStatus : UserStatus.PENDING;
        this.accountStatus = (accountStatus != null) ? accountStatus : AccountStatus.NORMAL;
        this.emailVerified = emailVerified;
        this.deleted = false;
    }
}
