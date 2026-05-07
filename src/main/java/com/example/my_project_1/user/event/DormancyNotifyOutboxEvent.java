package com.example.my_project_1.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DormancyNotifyOutboxEvent {
    private Long userId;
    private String email;
    private String nickname;
}
