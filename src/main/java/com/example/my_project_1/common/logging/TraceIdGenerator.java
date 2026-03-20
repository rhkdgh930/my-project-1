package com.example.my_project_1.common.logging;

import java.util.UUID;

public final class TraceIdGenerator {

    private TraceIdGenerator() {}

    public static String generate() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);
    }
}