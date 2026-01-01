package com.example.my_project_1.user.domain;

public enum UserStatus {
    PENDING, // 이메일 인증 전
    ACTIVE, // 기본 사용자
    SUSPENDED // 관리자에게 차단된 사용자
}
