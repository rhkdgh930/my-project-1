package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;
    private final Clock clock;

    @Override
    public void attachImages(
            Long ownerId,
            ImageOwnerType ownerType,
            List<String> storageKeys,
            Long uploaderId
    ) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }

        List<String> distinctKeys = storageKeys.stream()
                .distinct()
                .toList();

        List<Image> images =
                imageRepository.findAllByStorageKeyInAndUploaderId(distinctKeys, uploaderId);

        if (images.size() != distinctKeys.size()) {
            throw new IllegalArgumentException("존재하지 않거나 권한 없는 이미지 포함");
        }

        images.forEach(img -> img.attach(ownerId, ownerType));
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
        LocalDateTime now = LocalDateTime.now(clock);

        existing.stream()
                .filter(img -> !newKeySet.contains(img.getStorageKey()))
                .forEach(img -> img.detach(now));

        attachImages(ownerId, ownerType, newKeys, uploaderId);
    }

    @Override
    public void detachAll(Long ownerId, ImageOwnerType ownerType) {
        imageRepository.bulkDetachAll(ownerId, ownerType, LocalDateTime.now(clock));
    }
}
