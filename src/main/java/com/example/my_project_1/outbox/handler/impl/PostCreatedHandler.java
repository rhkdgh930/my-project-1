package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreatedHandler implements OutboxHandler {

    private final ImageService imageService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.POST_CREATED;
    }

    @Override
    public void handle(String payload) {
        PostCreatedEvent event =
                DataSerializer.deserialize(payload, PostCreatedEvent.class);

        log.debug(
                "[EVENT][PostCreatedHandler][ATTACH_IMAGES] postId={} imageCount={}",
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
}
