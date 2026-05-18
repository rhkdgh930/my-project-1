package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.monitoring.MonitoringService;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxProcessorTest {

    private final OutboxEventManager outboxEventManager = mock(OutboxEventManager.class);
    private final MonitoringService monitoringService = mock(MonitoringService.class);

    @Test
    @DisplayName("claim 성공한 이벤트만 handler를 실행하고 SUCCESS로 기록한다.")
    void process_handlesClaimedEventAndMarksSuccess() {
        TestHandler handler = handler();
        OutboxProcessor processor = new OutboxProcessor(outboxEventManager, monitoringService, List.of(handler));
        when(outboxEventManager.claim(1L))
                .thenReturn(new OutboxEventSnapshot(1L, OutboxEventType.USER_ACCOUNT_CHANGED, "{}"));

        processor.process(1L);

        verify(handler).handle("{}");
        verify(outboxEventManager).markSuccess(1L);
        verify(monitoringService).recordOutboxProcessSuccess(OutboxEventType.USER_ACCOUNT_CHANGED);
    }

    @Test
    @DisplayName("claim 실패 시 handler를 호출하지 않는다.")
    void process_skipsWhenClaimFails() {
        TestHandler handler = handler();
        OutboxProcessor processor = new OutboxProcessor(outboxEventManager, monitoringService, List.of(handler));
        when(outboxEventManager.claim(1L)).thenReturn(null);

        processor.process(1L);

        verify(handler, never()).handle("{}");
        verify(outboxEventManager, never()).markSuccess(1L);
        verify(outboxEventManager, never()).markFail(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("handler가 없으면 DEAD로 기록한다.")
    void process_marksDeadWhenHandlerMissing() {
        OutboxProcessor processorWithoutHandler = new OutboxProcessor(outboxEventManager, monitoringService, List.of());
        when(outboxEventManager.claim(1L))
                .thenReturn(new OutboxEventSnapshot(1L, OutboxEventType.USER_ACCOUNT_CHANGED, "{}"));

        processorWithoutHandler.process(1L);

        verify(outboxEventManager).markDead(1L, "HANDLER_NOT_FOUND");
        verify(monitoringService).recordOutboxProcessFail(OutboxEventType.USER_ACCOUNT_CHANGED);
    }

    @Test
    @DisplayName("handler 실패 시 FAILED로 기록한다.")
    void process_marksFailWhenHandlerThrows() {
        TestHandler handler = handler();
        OutboxProcessor processor = new OutboxProcessor(outboxEventManager, monitoringService, List.of(handler));
        RuntimeException exception = new RuntimeException("handler failed");
        when(outboxEventManager.claim(1L))
                .thenReturn(new OutboxEventSnapshot(1L, OutboxEventType.USER_ACCOUNT_CHANGED, "{}"));
        org.mockito.Mockito.doThrow(exception).when(handler).handle("{}");

        processor.process(1L);

        verify(outboxEventManager).markFail(1L, exception);
        verify(monitoringService).recordOutboxProcessFail(OutboxEventType.USER_ACCOUNT_CHANGED);
    }

    private interface TestHandler extends OutboxHandler {
    }

    private TestHandler handler() {
        TestHandler handler = mock(TestHandler.class);
        when(handler.getEventType()).thenReturn(OutboxEventType.USER_ACCOUNT_CHANGED);
        return handler;
    }
}
