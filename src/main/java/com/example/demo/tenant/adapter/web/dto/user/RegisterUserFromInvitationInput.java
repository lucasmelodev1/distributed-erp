package com.example.demo.tenant.adapter.web.dto.user;

import com.example.demo.user.domain.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserFromInvitationInput(
		@NotBlank @Size(max = 64) String code,
		@NotBlank @Size(max = User.NAME_MAX_SIZE) String name,
		@NotBlank @Size(min = User.PASSWORD_MIN_SIZE, max = User.PASSWORD_MAX_SIZE) String password) {
}
