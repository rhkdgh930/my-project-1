package com.example.my_project_1.user.domain;

public enum UserStatus {
    ACTIVE, // 기본 사용자
    WITHDRAWN_REQUESTED, // 탈퇴 요청
    WITHDRAWN, // 탈퇴 확정
    DORMANT // 휴면 사용자
}