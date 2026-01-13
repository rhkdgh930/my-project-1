package com.example.my_project_1.user.service.request;

import jakarta.persistence.Column;
import lombok.Getter;

@Getter
public class UserProfileUpdateRequest {
    private String introduce;

    private String profileImageUrl;
}
