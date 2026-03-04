package com.example.my_project_1.postimage.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
    String upload(MultipartFile file);

    void delete(String imageUrl);
}
