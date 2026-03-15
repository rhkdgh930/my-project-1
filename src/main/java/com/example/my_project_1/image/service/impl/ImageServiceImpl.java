package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;

    @Override
    public void attachImages(
            Long ownerId,
            ImageOwnerType ownerType,
            List<String> storageKeys,
            Long uploaderId
    ) {

        if (storageKeys == null || storageKeys.isEmpty()) return;

        List<Image> images =
                imageRepository.findAllByStorageKeyInAndUploaderId(storageKeys, uploaderId);

        images.stream()
                .filter(Image::isAttachable)
                .forEach(img -> img.attach(ownerId, ownerType));
    }

    @Override
    public void syncImages(
            Long ownerId,
            ImageOwnerType ownerType,
            List<String> newKeys,
            Long uploaderId
    ) {

        List<Image> existing =
                imageRepository.findAllByOwnerIdAndOwnerType(ownerId, ownerType);

        Set<String> newKeySet =
                new HashSet<>(newKeys != null ? newKeys : List.of());

        existing.stream()
                .filter(img -> !newKeySet.contains(img.getStorageKey()))
                .forEach(Image::detach);

        attachImages(ownerId, ownerType, newKeys, uploaderId);
    }

    @Override
    public void detachAll(Long ownerId, ImageOwnerType ownerType) {
        imageRepository.bulkDetachAll(ownerId, ownerType);
    }
}
