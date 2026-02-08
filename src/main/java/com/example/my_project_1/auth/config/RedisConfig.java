package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CachedUserContext> userContextRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, CachedUserContext> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. ObjectMapper 커스텀 설정
        ObjectMapper objectMapper = new ObjectMapper();

        // 🔥 Java 8 날짜/시간 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());

        // 🔥 시간을 타임스탬프(숫자)가 아닌 ISO-8601 문자열로 저장 (선택 사항이나 권장)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. 커스텀 ObjectMapper를 사용하는 Serializer 생성
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 3. Serializer 적용
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }
}
