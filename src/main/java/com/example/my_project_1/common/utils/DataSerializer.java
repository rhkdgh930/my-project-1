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
            log.error(
                    "[UTIL][DataSerializer][DESERIALIZE_FAIL] clazz={} reason={}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName(),
                    e
            );
            throw new IllegalStateException("JSON deserialization failed", e);
        }
    }

    public static <T> Optional<T> tryDeserialize(String data, Class<T> clazz) {
        try {
            return Optional.of(objectMapper.readValue(data, clazz));
        } catch (Exception e) {
            log.warn(
                    "[UTIL][DataSerializer][TRY_DESERIALIZE_FAIL] clazz={} dataLength={} reason={}",
                    clazz.getSimpleName(),
                    data != null ? data.length() : 0,
                    e.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    public static <T> T deserialize(InputStream inputStream, Class<T> clazz) {
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error(
                    "[UTIL][DataSerializer][DESERIALIZE_STREAM_FAIL] clazz={} reason={}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName(),
                    e
            );
            throw new IllegalStateException("JSON deserialization failed", e);
        }
    }

    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error(
                    "[UTIL][DataSerializer][SERIALIZE_FAIL] type={} reason={}",
                    object != null ? object.getClass().getSimpleName() : "null",
                    e.getClass().getSimpleName(),
                    e
            );
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
