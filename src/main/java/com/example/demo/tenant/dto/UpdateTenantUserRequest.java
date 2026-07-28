package com.example.demo.tenant.dto;

import java.util.Set;
import java.util.UUID;

public record UpdateTenantUserRequest(Set<UUID> tenantRoleIds) {

}
