package com.example.demo.tenant.adapter.web.dto.role;

import java.util.List;
import java.util.UUID;

import com.example.demo.tenant.domain.authorization.TenantRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTenantRoleInput(
		@NotNull UUID id,
		@NotBlank @Size(max = TenantRole.MAX_NAME_LENGTH) String name,
		@NotNull List<String> permissionCodes) {
}
