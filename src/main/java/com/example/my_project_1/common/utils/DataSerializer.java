package com.example.my_project_1.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataSerializer {
    private static final ObjectMapper objectMapper = initialize();

    private static ObjectMapper initialize() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T deserialize(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            log.error("[DataSerializer.deserialize] data={}, clazz={}", data, clazz, e);
            throw new IllegalStateException("JSON deserialization failed", e);
        }
    }

    public static <T> Optional<T> tryDeserialize(String data, Class<T> clazz) {
        try {
            return Optional.of(objectMapper.readValue(data, clazz));
        } catch (Exception e) {
            log.warn("[DataSerializer.tryDeserialize] data={}, clazz={} failed", data, clazz, e);
            return Optional.empty();
        }
    }

    public static <T> T deserialize(InputStream inputStream, Class<T> clazz) {
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("[DataSerializer.deserialize] inputStream={}, clazz={}", inputStream, clazz, e);
            throw new IllegalStateException("JSON deserialization failed", e);
        }
    }

    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[DataSerializer.serialize] object={}", object, e);
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
