package com.example.demo.tenant.authorization.tenantrole.dto;

import java.util.List;

import com.example.demo.tenant.authorization.tenantrole.TenantRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRoleRequest(
        @NotBlank @Size(max = TenantRole.MAX_NAME_LENGTH) String name,
        List<String> permissionCodes) {

}
