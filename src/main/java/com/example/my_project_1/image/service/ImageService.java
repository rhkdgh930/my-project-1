package com.example.my_project_1.image.service;

import com.example.my_project_1.image.domain.ImageOwnerType;

import java.util.List;

public interface ImageService {
    void attachImages(
            Long ownerId,
            ImageOwnerType ownerType,
            List<String> storageKeys,
            Long uploaderId
    );

    void syncImages(
            Long ownerId,
            ImageOwnerType ownerType,
            List<String> newKeys,
            Long uploaderId
    );

    void detachAll(Long ownerId, ImageOwnerType ownerType);
}
