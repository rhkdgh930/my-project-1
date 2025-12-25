package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        User user = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserSignUpResponse.from(user));
    }
}
