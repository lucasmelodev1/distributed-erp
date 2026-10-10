package com.example.demo.user.adapter.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordResetInput(
		@Email @NotBlank String email) {}
