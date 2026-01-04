package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.cache.CachedUserContext;
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

        // key serializer
        template.setKeySerializer(new StringRedisSerializer());

        // value serializer (JSON)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
