package com.example.my_project_1.common.config;

import com.example.my_project_1.common.logging.MdcTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "asyncTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        return createExecutor(
                5,
                10,
                500,
                "async-",
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "outboxExecutor")
    public Executor outboxExecutor() {
        return createExecutor(
                3,
                6,
                300,
                "outbox-async-",
                outboxRejectedExecutionHandler()
        );
    }

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        return createExecutor(
                2,
                6,
                200,
                "mail-async-",
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private ThreadPoolTaskExecutor createExecutor(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            String threadNamePrefix,
            RejectedExecutionHandler rejectedExecutionHandler
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        executor.initialize();
        return executor;
    }

    private RejectedExecutionHandler outboxRejectedExecutionHandler() {
        return (runnable, executor) ->
                log.warn(
                        "[OUTBOX][ASYNC_REJECTED] activeCount={} poolSize={} queueSize={}",
                        executor.getActiveCount(),
                        executor.getPoolSize(),
                        executor.getQueue().size()
                );
    }
}
