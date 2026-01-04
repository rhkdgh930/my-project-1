package com.example.my_project_1.user.domain;

public enum AccountStatus {
    NORMAL, // 기본 사용자
    SUSPENDED // 차단된 사용자
}
// 계정 상태 관리
// 삭제는 도메인에서 soft delete