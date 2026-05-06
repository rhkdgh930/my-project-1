package com.example.my_project_1.user.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작성자 표시 상태. ACTIVE, WITHDRAWN, SUSPENDED, UNKNOWN")
public enum AuthorStatus {
    ACTIVE,
    WITHDRAWN,
    SUSPENDED,
    UNKNOWN
}
