package com.fitflow.users.controller;

import com.fitflow.users.dto.UserProfileResponse;
import com.fitflow.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserProfileResponse getProfile(@PathVariable UUID id) {
        return userService.getProfile(id);
    }
}
