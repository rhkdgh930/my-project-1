package com.example.my_project_1.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetMailRequestedEvent {
    private String email;
    private String rawToken;
    private String resetLink;
}
