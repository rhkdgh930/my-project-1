package com.example.my_project_1.common.config;

import com.example.my_project_1.outbox.listener.OutboxEventListener;
import com.example.my_project_1.user.listener.EmailVerificationMailEventListener;
import com.example.my_project_1.user.listener.PasswordResetMailEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    @DisplayName("Outbox와 mail 비동기 작업은 서로 다른 executor를 사용한다.")
    void listeners_useSeparatedExecutors() throws Exception {
        Method outboxMethod = OutboxEventListener.class.getDeclaredMethod(
                "handle",
                Class.forName("com.example.my_project_1.outbox.listener.OutboxSavedEvent")
        );
        Method passwordResetMailMethod = PasswordResetMailEventListener.class.getDeclaredMethod(
                "sendPasswordResetMail",
                Class.forName("com.example.my_project_1.user.event.PasswordResetMailRequestedEvent")
        );
        Method emailVerificationMailMethod = EmailVerificationMailEventListener.class.getDeclaredMethod(
                "sendVerificationMail",
                Class.forName("com.example.my_project_1.user.event.EmailVerificationMailRequestedEvent")
        );

        assertThat(outboxMethod.getAnnotation(Async.class).value()).isEqualTo("outboxExecutor");
        assertThat(passwordResetMailMethod.getAnnotation(Async.class).value()).isEqualTo("mailExecutor");
        assertThat(emailVerificationMailMethod.getAnnotation(Async.class).value()).isEqualTo("mailExecutor");
    }

    @Test
    @DisplayName("outboxExecutor는 별도 thread prefix와 CallerRuns가 아닌 rejection policy를 사용한다.")
    void outboxExecutor_hasSeparatedConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.outboxExecutor();

        try {
            assertThat(executor.getThreadNamePrefix()).isEqualTo("outbox-async-");
            assertThat(executor.getCorePoolSize()).isEqualTo(3);
            assertThat(executor.getMaxPoolSize()).isEqualTo(6);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isNotInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("mailExecutor는 별도 thread prefix와 CallerRuns rejection policy를 사용한다.")
    void mailExecutor_hasSeparatedConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.mailExecutor();

        try {
            assertThat(executor.getThreadNamePrefix()).isEqualTo("mail-async-");
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(6);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("분리된 executor에서도 MDC가 전파된다.")
    void separatedExecutors_propagateMdc() throws Exception {
        ThreadPoolTaskExecutor outboxExecutor = (ThreadPoolTaskExecutor) asyncConfig.outboxExecutor();
        ThreadPoolTaskExecutor mailExecutor = (ThreadPoolTaskExecutor) asyncConfig.mailExecutor();

        try {
            MDC.put("traceId", "trace-1");
            Future<String> outboxTraceId = outboxExecutor.submit(() -> MDC.get("traceId"));
            Future<String> mailTraceId = mailExecutor.submit(() -> MDC.get("traceId"));
            MDC.clear();

            assertThat(outboxTraceId.get()).isEqualTo("trace-1");
            assertThat(mailTraceId.get()).isEqualTo("trace-1");
        } finally {
            MDC.clear();
            outboxExecutor.shutdown();
            mailExecutor.shutdown();
        }
    }
}
