package com.example.demo.tenant.adapter.web.dto.user;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteUserToTenantRequest(
		@NotBlank @Email String email,
		Set<UUID> tenantRoles) {

}
