package com.example.demo.user.adapter.web.dto.auth;

import com.example.demo.user.domain.AuthToken;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPasswordResetRequestRequest(
		@Schema(description = "Password Reset Request Token", example = "123456")
		@Size(min = AuthToken.TOKEN_SIZE, max = AuthToken.TOKEN_SIZE, message = "Token with invalid size")
		@NotBlank
		String token) {}
