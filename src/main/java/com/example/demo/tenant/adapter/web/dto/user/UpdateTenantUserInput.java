package com.example.demo.tenant.adapter.web.dto.user;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantUserInput(@NotNull UUID userId, @NotNull List<UUID> tenantRoleIds) {
}
