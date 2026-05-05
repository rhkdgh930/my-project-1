package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.post.event.PostDeletedOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostDeletedHandler implements OutboxHandler {

    private final ImageService imageService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.POST_DELETED;
    }

    @Override
    public void handle(String payload) {
        PostDeletedOutboxEvent event =
                DataSerializer.deserialize(payload, PostDeletedOutboxEvent.class);

        if (event == null || event.getPostId() == null || event.getUserId() == null) {
            throw new IllegalArgumentException("Invalid POST_DELETED payload");
        }

        log.debug(
                "[EVENT][PostDeletedHandler][DETACH_IMAGES] postId={}",
                event.getPostId()
        );

        imageService.syncImages(
                event.getPostId(),
                ImageOwnerType.POST,
                List.of(),
                event.getUserId()
        );
    }
}
