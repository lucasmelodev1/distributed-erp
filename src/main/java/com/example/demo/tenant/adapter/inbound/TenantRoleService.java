package com.example.demo.tenant.adapter.inbound;
import com.example.demo.tenant.port.inbound.TenantRoleManagementPort;

import java.util.UUID;
import java.util.stream.Collectors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import com.example.demo.tenant.adapter.outbound.persistence.PermissionRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository.RolePermissionRow;
import com.example.demo.tenant.domain.authorization.Permission;
import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleInput;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleInput;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.domain.authorization.TenantRolePermission;
import com.example.demo.tenant.domain.authorization.TenantRoleSpecification;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;

@Service
public class TenantRoleService implements TenantRoleManagementPort {
    private static final Logger log = LoggerFactory.getLogger(TenantRoleService.class);
    private final TenantRoleRepository tenantRoleRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public TenantRoleService(TenantRoleRepository tenantRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionRepository permissionRepository) {
        this.tenantRoleRepository = tenantRoleRepository;
        this.tenantRolePermissionRepository = tenantRolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    private void requireKnownPermissions(Collection<String> codes) {
        var known = permissionRepository.findAllById(codes).stream().map(Permission::getCode).collect(Collectors.toSet());
        var unknown = codes.stream().filter(code -> !known.contains(code)).sorted().distinct().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown permission codes: " + String.join(", ", unknown));
        }
    }

    @Transactional(readOnly = true)
    public Window<TenantRole> scrollTenantRoles(@Nullable String query, @Nullable Boolean active, int limit,
            @Nullable UUID after, UUID tenantId) {
        var spec = Specification.allOf(
                TenantRoleSpecification.hasTenantId(tenantId),
                TenantRoleSpecification.hasActiveStatus(active),
                TenantRoleSpecification.hasName(query));
        ScrollPosition position = after == null ? ScrollPosition.keyset()
                : ScrollPosition.forward(Map.of("id", after));
        return tenantRoleRepository.findBy(spec, q -> q.sortBy(Sort.by("id")).limit(limit).scroll(position));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<Permission>> findPermissionsByRoleIds(Collection<UUID> roleIds, UUID tenantId) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return tenantRolePermissionRepository.findPermissionsByRoleIds(roleIds, tenantId).stream()
                .collect(Collectors.groupingBy(RolePermissionRow::roleId,
                        Collectors.mapping(RolePermissionRow::permission, Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public TenantRole findTenantRoleById(UUID id, UUID tenantId) {
        var tenantRole = tenantRoleRepository.findByIdAndTenantId(id, tenantId);
        if (tenantRole.isEmpty()) {
            throw new EntityNotFoundException("Tenant role not found for id: " + id);
        }
        tenantRole.get().getPermissions().size(); // Force loading of permissions
        return tenantRole.get();
    }

    @Transactional
    public TenantRole createTenantRole(CreateTenantRoleInput dto, UUID tenantId) {
        requireKnownPermissions(dto.permissionCodes());
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

        return tenantRole;
    }

    @Transactional
    public TenantRole updateTenantRole(UpdateTenantRoleInput dto, UUID tenantId) {
        var tenantRoleQueryResult = tenantRoleRepository.findByIdAndTenantId(dto.id(), tenantId);
        if (tenantRoleQueryResult.isEmpty()) {
            throw new EntityNotFoundException("Tenant role not found for id: " + dto.id());
        }
        var tenantRole = tenantRoleQueryResult.get();

        requireKnownPermissions(dto.permissionCodes());
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
            return tenantRoleRepository.save(tenantRole);
        } catch (Exception e) {
            log.info("Error updating tenant role for tenant={} with name={}: {}", tenantId, dto.name(), e.getMessage(),
                    e);
            throw e;
        }
    }

    @Transactional
    public TenantRole setTenantRoleActive(UUID id, boolean active, UUID tenantId) {
        var tenantRole = tenantRoleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant role not found for id: " + id));
        if (active) {
            tenantRole.reactivate();
        } else {
            tenantRole.inactivate();
        }
        log.info("Tenant role={} in tenant={} is now {}", id, tenantId, active ? "active" : "inactive");
        return tenantRoleRepository.save(tenantRole);
    }
}
