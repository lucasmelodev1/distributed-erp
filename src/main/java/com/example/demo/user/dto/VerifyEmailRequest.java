package com.example.demo.user.dto;

import com.example.demo.user.AuthToken;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
		@Schema(description = "Verify Email Token", example = "123456")
		@Size(min = AuthToken.TOKEN_SIZE, max = AuthToken.TOKEN_SIZE, message = "Token with invalid size")
		@NotBlank
		String token) {}
