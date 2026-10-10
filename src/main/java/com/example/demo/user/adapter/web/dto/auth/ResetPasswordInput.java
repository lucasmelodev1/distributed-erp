package com.example.demo.user.adapter.web.dto.auth;

import com.example.demo.user.domain.AuthToken;
import com.example.demo.user.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordInput(
		@Email @NotBlank String email,
		@Size(min = AuthToken.TOKEN_SIZE, max = AuthToken.TOKEN_SIZE) @NotBlank String token,
		@Size(min = User.PASSWORD_MIN_SIZE, max = User.PASSWORD_MAX_SIZE) @NotBlank String password) {}
