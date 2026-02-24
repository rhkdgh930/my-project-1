package com.example.my_project_1.postimage.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.postimage.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping
    public ResponseEntity<?> upload(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam MultipartFile file
    ) {
        Long uploaderId = userDetails.getUserId();
        String imageUrl = imageUploadService.upload(file, uploaderId);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}
