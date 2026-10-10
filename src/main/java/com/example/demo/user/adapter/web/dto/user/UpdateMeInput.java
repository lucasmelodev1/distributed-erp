package com.example.demo.user.adapter.web.dto.user;

import com.example.demo.user.domain.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeInput(
        @Size(max = User.NAME_MAX_SIZE) @NotBlank String name) {

}
