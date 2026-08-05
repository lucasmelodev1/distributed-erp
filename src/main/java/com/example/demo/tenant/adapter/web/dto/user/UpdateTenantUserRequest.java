package com.example.demo.tenant.adapter.web.dto.user;

import java.util.Set;
import java.util.UUID;

public record UpdateTenantUserRequest(Set<UUID> tenantRoleIds) {

}
