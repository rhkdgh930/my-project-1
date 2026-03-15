package com.example.my_project_1.common.logging;

import org.slf4j.MDC;

import java.util.UUID;

public class BatchTraceHelper {

    public static void start() {
        String traceId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16);
        MDC.put(TraceIdFilter.TRACE_ID, traceId);

    }

    public static void clear() {
        MDC.clear();

    }
}