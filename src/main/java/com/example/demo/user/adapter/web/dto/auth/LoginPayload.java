package com.example.demo.user.adapter.web.dto.auth;

import java.util.UUID;

public record LoginPayload(
		UUID id,
		String email,
		String role) {}
