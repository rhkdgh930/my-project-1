package com.example.my_project_1.user.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차단 유형. TEMPORARY=일시 차단, PERMANENT=영구 차단")
public enum SuspensionType {
    TEMPORARY,
    PERMANENT
}
