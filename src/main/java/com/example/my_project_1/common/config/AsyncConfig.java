package com.example.my_project_1.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. 코어 스레드 수 (항상 유지할 스레드 개수)
        executor.setCorePoolSize(5);

        // 2. 최대 스레드 수 (대기열이 꽉 찼을 때 추가로 생성할 최대 개수)
        executor.setMaxPoolSize(10);

        // 3. 대기열 용량 (스레드가 꽉 찼을 때 작업을 쌓아둘 공간)
        executor.setQueueCapacity(500);

        // 4. 스레드 이름 접두사 (로그에서 비동기 스레드 확인용)
        executor.setThreadNamePrefix("AsyncThread-");

        executor.initialize();

        return executor;
    }
}