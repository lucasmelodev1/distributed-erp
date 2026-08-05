package com.example.demo.tenant.adapter.inbound;
import com.example.demo.tenant.port.inbound.TenantRoleManagementPort;

import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleRequest;
import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleResponse;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleRequest;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.domain.authorization.TenantRolePermission;
import com.example.demo.tenant.domain.authorization.TenantRoleSpecification;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;

@Service
public class TenantRoleService implements TenantRoleManagementPort {
    private static final Logger log = LoggerFactory.getLogger(TenantRoleService.class);
    private final TenantRoleRepository tenantRoleRepository;

    public TenantRoleService(TenantRoleRepository tenantRoleRepository) {
        this.tenantRoleRepository = tenantRoleRepository;
    }

    @Transactional(readOnly = true)
    public Page<TenantRole> findAllTenantRoles(@Nullable String query, Pageable pageable, UUID tenantId) {
        var spec = Specification.allOf(
                TenantRoleSpecification.hasTenantId(tenantId),
                TenantRoleSpecification.hasName(query));
        var result = tenantRoleRepository.findAll(spec, pageable);
        result.forEach(role -> Hibernate.initialize(role.getPermissions()));
        return result;
    }

    @Transactional(readOnly = true)
    public TenantRole findTenantRoleById(UUID id, UUID tenantId) {
        var tenantRole = tenantRoleRepository.findByIdAndTenantId(id, tenantId);
        if (tenantRole.isEmpty()) {
            throw new IllegalArgumentException("Tenant role not found for id: " + id);
        }
        tenantRole.get().getPermissions().size(); // Force loading of permissions
        return tenantRole.get();
    }

    @Transactional
    public CreateTenantRoleResponse createTenantRole(CreateTenantRoleRequest dto, UUID tenantId) {
        var permissions = dto
                .permissionCodes()
                .stream()
                .map(code -> {
                    var p = new TenantRolePermission(null, code, tenantId);
                    return p;
                })
                .collect(Collectors.toSet());
        var tenantRole = new TenantRole(tenantId, dto.name(), permissions);
        permissions.forEach(p -> p.setRole(tenantRole));
        try {
            tenantRoleRepository.save(tenantRole);
        } catch (Exception e) {
            log.info("Error creating tenant role for tenant={} with name={}: {}", tenantId, dto.name(), e.getMessage(),
                    e);
            throw e;
        }

        return new CreateTenantRoleResponse(tenantRole.getId());
    }

    @Transactional
    public void updateTenantRole(UUID id, UpdateTenantRoleRequest dto, UUID tenantId) {
        var tenantRoleQueryResult = tenantRoleRepository.findByIdAndTenantId(id, tenantId);
        if (tenantRoleQueryResult.isEmpty()) {
            throw new IllegalArgumentException("Tenant role not found for id: " + id);
        }
        var tenantRole = tenantRoleQueryResult.get();

        tenantRole.setName(dto.name());
        tenantRole.getPermissions().clear();
        tenantRoleRepository.flush();
        var permissions = dto
                .permissionCodes()
                .stream()
                .map(code -> new TenantRolePermission(tenantRole, code, tenantId))
                .collect(Collectors.toSet());
        tenantRole.getPermissions().addAll(permissions);

        try {
            tenantRoleRepository.save(tenantRole);
        } catch (Exception e) {
            log.info("Error updating tenant role for tenant={} with name={}: {}", tenantId, dto.name(), e.getMessage(),
                    e);
            throw e;
        }
    }

    @Transactional
    public void deleteTenantRole(UUID id, UUID tenantId) {
        var tenantRoleQueryResult = tenantRoleRepository.findByIdAndTenantId(id, tenantId);
        if (tenantRoleQueryResult.isEmpty()) {
            throw new IllegalArgumentException("Tenant role not found for id: " + id);
        }
        var tenantRole = tenantRoleQueryResult.get();
        try {
            tenantRoleRepository.delete(tenantRole);
        } catch (Exception e) {
            log.info("Error deleting tenant role for tenant={} with id={}: {}", tenantId, id, e.getMessage(), e);
            throw e;
        }
    }
}
