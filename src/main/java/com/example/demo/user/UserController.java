package com.example.demo.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.dto.FindMeResponse;
import com.example.demo.user.dto.UpdateUserRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "User", description = "User management")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Find the current user")
    public FindMeResponse findMe(@AuthenticationPrincipal User user) {
        return userService.findMe(user);
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user")
    public void updateMe(
            @Valid @RequestBody UpdateUserRequest dto,
            @AuthenticationPrincipal User user) {
        userService.updateUser(dto, user);
    }
}
