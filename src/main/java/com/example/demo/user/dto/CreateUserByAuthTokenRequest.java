package com.example.demo.user.dto;

import com.example.demo.user.AuthToken;
import com.example.demo.user.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserByAuthTokenRequest(
		@Schema(description = "User name", example = "Stephany King") @Size(max = User.NAME_MAX_SIZE) @NotBlank String name,
		@Schema(description = "Create User Token", example = "123456") @Size(min = AuthToken.TOKEN_SIZE, max = AuthToken.TOKEN_SIZE) @NotBlank String token,
		@Schema(description = "Password", example = "Password@123") @Size(min = User.PASSWORD_MIN_SIZE, max = User.PASSWORD_MAX_SIZE) @NotBlank String password) {
}
