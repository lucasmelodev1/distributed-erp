package com.example.demo.tenant.port.inbound;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Window;
import com.example.demo.tenant.domain.authorization.Permission;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleInput;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleInput;
import com.example.demo.tenant.domain.authorization.TenantRole;

public interface TenantRoleManagementPort {
    /** Forward keyset page ordered by id (creation time). */
    Window<TenantRole> scrollTenantRoles(@Nullable String query, @Nullable Boolean active, int limit, @Nullable UUID after, UUID tenantId);
    Map<UUID, List<Permission>> findPermissionsByRoleIds(Collection<UUID> roleIds, UUID tenantId);
    TenantRole findTenantRoleById(UUID id, UUID tenantId);
    TenantRole createTenantRole(CreateTenantRoleInput dto, UUID tenantId);
    TenantRole updateTenantRole(UpdateTenantRoleInput dto, UUID tenantId);
    TenantRole setTenantRoleActive(UUID id, boolean active, UUID tenantId);
    /** Every permission that exists, ordered by code. */
    List<Permission> findAllPermissions();
}
