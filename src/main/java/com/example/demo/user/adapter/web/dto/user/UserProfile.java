package com.example.demo.user.adapter.web.dto.user;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record UserProfile(
		UUID id,
		String name,
		String email,
		@Nullable String profileIconUrl) {

}
