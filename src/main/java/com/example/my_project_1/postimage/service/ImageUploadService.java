package com.example.my_project_1.postimage.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
    String upload(MultipartFile file, Long uploaderId);
}
