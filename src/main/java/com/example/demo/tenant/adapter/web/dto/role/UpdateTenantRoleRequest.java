package com.example.demo.tenant.adapter.web.dto.role;

import java.util.Set;

import com.example.demo.tenant.domain.authorization.TenantRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRoleRequest(
        @NotBlank @Size(max = TenantRole.MAX_NAME_LENGTH) String name,
        Set<String> permissionCodes) {

}
