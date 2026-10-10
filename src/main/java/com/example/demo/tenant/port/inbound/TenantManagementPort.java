package com.example.demo.tenant.port.inbound;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantInput;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantInput;
import com.example.demo.tenant.domain.Tenant;

public interface TenantManagementPort {
    List<Tenant> findAllUserTenants(UUID userId);
    Tenant createTenant(CreateTenantInput dto, UUID userId);
    Tenant updateTenant(UpdateTenantInput dto, UUID tenantId);
    Tenant setTenantActive(UUID tenantId, boolean active);
    Optional<Tenant> findTenantById(UUID tenantId);
}
