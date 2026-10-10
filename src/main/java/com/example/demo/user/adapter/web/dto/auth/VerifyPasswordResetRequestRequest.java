package com.example.demo.user.adapter.web.dto.auth;

import com.example.demo.user.domain.AuthToken;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPasswordResetRequestRequest(
		@Size(min = AuthToken.TOKEN_SIZE, max = AuthToken.TOKEN_SIZE, message = "Token with invalid size")
		@NotBlank
		String token) {}
