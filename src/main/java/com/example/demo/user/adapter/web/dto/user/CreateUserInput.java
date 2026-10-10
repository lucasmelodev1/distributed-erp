package com.example.demo.user.adapter.web.dto.user;

import com.example.demo.user.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserInput(
        @Size(max = User.NAME_MAX_SIZE) @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = User.PASSWORD_MIN_SIZE, max = User.PASSWORD_MAX_SIZE) @NotBlank String password) {
}