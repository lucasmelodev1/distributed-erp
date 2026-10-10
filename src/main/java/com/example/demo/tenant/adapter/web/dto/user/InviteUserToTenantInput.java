package com.example.demo.tenant.adapter.web.dto.user;

import java.util.List;
import java.util.UUID;

import com.example.demo.utils.validations.StrictEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteUserToTenantInput(@NotBlank @StrictEmail String email, @NotNull List<UUID> tenantRoleIds) {
}
