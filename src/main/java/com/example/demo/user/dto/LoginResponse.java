package com.example.demo.user.dto;

import java.util.UUID;

public record LoginResponse(
		UUID id,
		String email,
		String role) {}
