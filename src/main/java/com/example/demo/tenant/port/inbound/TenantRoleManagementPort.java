package com.example.demo.tenant.port.inbound;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Window;
import com.example.demo.tenant.domain.authorization.Permission;
import org.springframework.data.domain.Pageable;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleRequest;
import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleResponse;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleRequest;
import com.example.demo.tenant.domain.authorization.TenantRole;

public interface TenantRoleManagementPort {
    Page<TenantRole> findAllTenantRoles(@Nullable String query, Pageable pageable, UUID tenantId);
    /** Forward keyset page ordered by id (creation time). */
    Window<TenantRole> scrollTenantRoles(@Nullable String query, @Nullable Boolean active, int limit, @Nullable UUID after, UUID tenantId);
    Map<UUID, List<Permission>> findPermissionsByRoleIds(Collection<UUID> roleIds, UUID tenantId);
    TenantRole findTenantRoleById(UUID id, UUID tenantId);
    CreateTenantRoleResponse createTenantRole(CreateTenantRoleRequest dto, UUID tenantId);
    void updateTenantRole(UUID id, UpdateTenantRoleRequest dto, UUID tenantId);
    void deleteTenantRole(UUID id, UUID tenantId);
    TenantRole setTenantRoleActive(UUID id, boolean active, UUID tenantId);
}
