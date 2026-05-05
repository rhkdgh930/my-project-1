package com.example.my_project_1.image.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageUrlParser {

    private static final String PREFIX = "/images/";

    private static final Pattern IMAGE_PATTERN =
            Pattern.compile("!\\[[^\\]]*\\]\\((.*?)\\)");

    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(png|jpg|jpeg|gif|webp)",
                    Pattern.CASE_INSENSITIVE
            );

    private ImageUrlParser() {
    }

    public static List<String> extractStorageKeys(String content) {
        if (content == null) return List.of();

        List<String> urls = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            extractStorageKey(matcher.group(1))
                    .ifPresent(urls::add);
        }
        return urls.stream().distinct().toList();
    }

    private static java.util.Optional<String> extractStorageKey(String url) {
        if (url == null || !url.startsWith(PREFIX)) {
            return java.util.Optional.empty();
        }

        String storageKey = url.substring(PREFIX.length());
        if (!STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(storageKey);
    }
}
