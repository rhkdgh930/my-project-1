package com.example.my_project_1.image.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUrlParserTest {

    private static final String STORAGE_KEY = "123e4567-e89b-12d3-a456-426614174000.png";

    @Test
    @DisplayName("extractStorageKeys extracts only internal image storage keys")
    void extractStorageKeys_extractsOnlyInternalImageStorageKeys() {
        String content = "![image](/images/%s)".formatted(STORAGE_KEY);

        List<String> storageKeys = ImageUrlParser.extractStorageKeys(content);

        assertThat(storageKeys).containsExactly(STORAGE_KEY);
    }

    @Test
    @DisplayName("extractStorageKeys ignores external and malformed image URLs")
    void extractStorageKeys_ignoresExternalAndMalformedImageUrls() {
        String content = """
                ![external](https://cdn.example.com/image.png)
                ![absolute](https://host/images/%s)
                ![middle](/foo/images/%s)
                ![query](/images/%s?v=1)
                ![fragment](/images/%s#fragment)
                ![encoded](/images/%%2e%%2e%%2fsecret.png)
                ![slash](/images/../secret.png)
                ![backslash](/images/..\\secret.png)
                """.formatted(
                STORAGE_KEY,
                STORAGE_KEY,
                STORAGE_KEY,
                STORAGE_KEY
        );

        List<String> storageKeys = ImageUrlParser.extractStorageKeys(content);

        assertThat(storageKeys).isEmpty();
    }

    @Test
    @DisplayName("extractStorageKeys removes duplicate internal storage keys")
    void extractStorageKeys_removesDuplicateInternalStorageKeys() {
        String content = """
                ![first](/images/%s)
                ![second](/images/%s)
                """.formatted(STORAGE_KEY, STORAGE_KEY);

        List<String> storageKeys = ImageUrlParser.extractStorageKeys(content);

        assertThat(storageKeys).containsExactly(STORAGE_KEY);
    }
}
