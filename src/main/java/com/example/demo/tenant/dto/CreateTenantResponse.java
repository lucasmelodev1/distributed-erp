package com.example.demo.tenant.dto;

import java.util.UUID;

public record CreateTenantResponse(
		UUID tenantId,
		UUID tenantUserId) {
}
