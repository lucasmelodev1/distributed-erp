package com.example.demo.user.adapter.web.dto.auth;

import com.example.demo.utils.validations.StrictEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RequestEmailVerificationRequest(
		@Schema(description = "Email address", example = "demo@example.com") @NotBlank @StrictEmail String email) {
}
