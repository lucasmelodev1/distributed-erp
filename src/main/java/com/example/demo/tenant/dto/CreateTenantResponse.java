package com.example.demo.tenant.dto;

import java.util.List;
import java.util.UUID;

public record CreateTenantResponse(
		UUID id,
		String name,
		List<UUID> users,
		List<UUID> roles) {
}
