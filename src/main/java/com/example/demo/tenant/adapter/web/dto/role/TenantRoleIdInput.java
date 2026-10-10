package com.example.demo.tenant.adapter.web.dto.role;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TenantRoleIdInput(@NotNull UUID id) {
}
