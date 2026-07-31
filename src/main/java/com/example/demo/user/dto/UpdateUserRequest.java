package com.example.demo.user.dto;

import com.example.demo.user.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = User.NAME_MAX_SIZE) @NotBlank String name) {

}
