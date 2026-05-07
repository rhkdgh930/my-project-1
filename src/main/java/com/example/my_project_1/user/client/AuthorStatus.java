package com.example.my_project_1.user.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작성자 표시 상태. ACTIVE=정상 작성자, WITHDRAWN=탈퇴한 사용자, SUSPENDED=차단된 사용자, UNKNOWN=작성자 조회 실패 또는 사용자 없음")
public enum AuthorStatus {
    ACTIVE,
    WITHDRAWN,
    SUSPENDED,
    UNKNOWN
}
