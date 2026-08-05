package com.example.demo.tenant.port.inbound;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleRequest;
import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleResponse;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleRequest;
import com.example.demo.tenant.domain.authorization.TenantRole;

public interface TenantRoleManagementPort {
    Page<TenantRole> findAllTenantRoles(@Nullable String query, Pageable pageable, UUID tenantId);
    TenantRole findTenantRoleById(UUID id, UUID tenantId);
    CreateTenantRoleResponse createTenantRole(CreateTenantRoleRequest dto, UUID tenantId);
    void updateTenantRole(UUID id, UpdateTenantRoleRequest dto, UUID tenantId);
    void deleteTenantRole(UUID id, UUID tenantId);
}
