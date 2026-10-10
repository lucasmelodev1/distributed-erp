package com.example.demo.user.adapter.web.dto.auth;

import com.example.demo.utils.validations.StrictEmail;

import jakarta.validation.constraints.NotBlank;

public record RequestEmailVerificationRequest(
		@NotBlank @StrictEmail String email) {
}
