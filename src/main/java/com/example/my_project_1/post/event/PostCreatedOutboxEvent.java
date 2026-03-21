package com.example.my_project_1.post.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCreatedOutboxEvent {

    private Long postId;
    private Long userId;
    private List<String> storageKeys;

}