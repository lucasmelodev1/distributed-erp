package com.example.demo.tenant.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteUserToTenantRequest(
		@NotBlank @Email String email,
		List<UUID> tenantRoles) {

}
