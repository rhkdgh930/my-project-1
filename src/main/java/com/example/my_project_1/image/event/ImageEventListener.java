package com.example.my_project_1.image.event;

import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.post.event.PostCreatedEvent;
import com.example.my_project_1.post.event.PostUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ImageEventListener {

    private final ImageService imageService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        imageService.attachImages(
                event.getPostId(),
                ImageOwnerType.POST,
                event.getStorageKeys(),
                event.getUserId()
        );

    }

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostUpdated(PostUpdatedEvent event) {
        imageService.syncImages(
                event.getPostId(),
                ImageOwnerType.POST,
                event.getStorageKeys(),
                event.getUserId()
        );

    }

}