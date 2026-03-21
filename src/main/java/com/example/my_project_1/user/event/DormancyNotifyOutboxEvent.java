package com.example.my_project_1.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DormancyNotifyOutboxEvent {
    private Long userId;
    private String email;
    private String nickname;
}
