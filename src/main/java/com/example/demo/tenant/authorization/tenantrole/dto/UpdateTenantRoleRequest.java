package com.example.demo.tenant.authorization.tenantrole.dto;

import java.util.Set;

import com.example.demo.tenant.authorization.tenantrole.TenantRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRoleRequest(
        @NotBlank @Size(max = TenantRole.MAX_NAME_LENGTH) String name,
        Set<String> permissionCodes) {

}
