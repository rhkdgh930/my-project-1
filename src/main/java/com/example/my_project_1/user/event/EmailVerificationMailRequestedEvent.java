package com.example.my_project_1.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationMailRequestedEvent {
    private String email;
    private String code;
}
