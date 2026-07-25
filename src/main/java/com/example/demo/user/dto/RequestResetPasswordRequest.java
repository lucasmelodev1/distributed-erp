package com.example.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestResetPasswordRequest(
		@Schema(description = "Email address", example = "demo@example.com")
		@Email @NotBlank String email) {}
