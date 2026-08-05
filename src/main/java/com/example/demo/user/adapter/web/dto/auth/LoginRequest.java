package com.example.demo.user.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@Schema(description = "Email address", example = "demo@example.com") @Email @NotBlank String email,
		@Schema(description = "Password", example = "Password@123") @NotBlank String password) {
}
