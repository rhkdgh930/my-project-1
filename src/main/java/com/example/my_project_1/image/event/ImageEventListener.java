package com.example.my_project_1.image.event;

import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.post.event.PostCreatedEvent;
import com.example.my_project_1.post.event.PostUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageEventListener {

    private final ImageService imageService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        log.debug(
                "[EVENT][ImageEventListener][ATTACH_IMAGES] postId={} imageCount={}",
                event.getPostId(),
                event.getStorageKeys().size()
        );
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
        log.debug(
                "[EVENT][ImageEventListener][SYNC_IMAGES] postId={} imageCount={}",
                event.getPostId(),
                event.getStorageKeys().size()
        );
        imageService.syncImages(
                event.getPostId(),
                ImageOwnerType.POST,
                event.getStorageKeys(),
                event.getUserId()
        );

    }

}