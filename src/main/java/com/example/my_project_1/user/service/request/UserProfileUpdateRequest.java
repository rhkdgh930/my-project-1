package com.example.my_project_1.user.service.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;

@Getter
public class UserProfileUpdateRequest {
    @Size(max = 500)
    private String introduce;

    @URL
    private String profileImageUrl;
}
