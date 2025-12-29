package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import jakarta.persistence.*;
import lombok.*;

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
    private UserStatus status;

    private boolean emailVerified;

    private LocalDateTime lastLoginAt;

    private boolean deleted;

    public static User signUp(UserSignUpRequest request, String encodedPassword) {
        User user = new User();
        user.email = request.getEmail();
        user.password = encodedPassword;
        user.nickname = request.getNickname();
        user.role = Role.USER;
        user.status = UserStatus.PENDING;
        return user;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}
