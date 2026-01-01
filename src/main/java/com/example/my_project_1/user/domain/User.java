package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean deleted;

    private LocalDateTime lastLoginAt;

    public static User signUp(UserSignUpRequest request, String encodedPassword) {
        User user = new User();
        user.email = request.getEmail();
        user.password = encodedPassword;
        user.nickname = request.getNickname();
        user.role = Role.USER;
        user.userStatus = UserStatus.PENDING;
        user.accountStatus = AccountStatus.NORMAL;
        user.emailVerified = false;
        user.deleted = false;
        return user;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void withdraw() {
        if (this.deleted || this.accountStatus == AccountStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }
        this.deleted = true;
        this.accountStatus = AccountStatus.WITHDRAWN;
    }

    public void verifyEmail() {
        if (this.userStatus != UserStatus.PENDING) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED_USER);
        }
        this.emailVerified = true;
        this.userStatus = UserStatus.ACTIVE;
    }

    public static User createSuperUser(String encodedPassword) {
        User user = new User();
        user.email = "super@super.com";
        user.password = encodedPassword;
        user.nickname = "super";
        user.role = Role.ADMIN;
        user.userStatus = UserStatus.ACTIVE;
        user.accountStatus = AccountStatus.NORMAL;
        user.emailVerified = true;
        user.deleted = false;
        return user;
    }
}
