package com.example.my_project_1.common.logging;

import org.slf4j.MDC;

import java.util.UUID;

public class BatchTraceHelper {

    public static void start() {
        MDC.put(TraceConstants.TRACE_ID, TraceIdGenerator.generate());
    }

    public static void clear() {
        MDC.clear();
    }
}