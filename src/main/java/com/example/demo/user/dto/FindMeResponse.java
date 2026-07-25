package com.example.demo.user.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record FindMeResponse(
		UUID id,
		String name,
		String email,
		@Nullable String profileIconUrl) {

}
