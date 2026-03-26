package com.example.my_project_1.image.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageUrlParser {

    private static final String PREFIX = "/images/";

    private static final Pattern IMAGE_PATTERN =
            Pattern.compile("!\\[[^\\]]*\\]\\((.*?)\\)");

    private ImageUrlParser() {
    }

    public static List<String> extractStorageKeys(String content) {
        if (content == null) return List.of();

        List<String> urls = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            urls.add(
                    matcher.group(1).replace(PREFIX, "")
            );
        }
        return urls.stream().distinct().toList();
    }
}
