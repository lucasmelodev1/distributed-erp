package com.example.demo.tenant.port.inbound;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantRequest;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantResponse;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantRequest;
import com.example.demo.tenant.domain.Tenant;

public interface TenantManagementPort {
    List<Tenant> findAllUserTenants(UUID userId);
    CreateTenantResponse createTenant(CreateTenantRequest dto, UUID userId);
    Tenant updateTenant(UpdateTenantRequest dto, UUID tenantId);
    Tenant setTenantActive(UUID tenantId, boolean active);
    Optional<Tenant> findTenantById(UUID tenantId);
}
