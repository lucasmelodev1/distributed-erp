package com.example.demo.tenant.adapter.inbound;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.example.demo.tenant.adapter.outbound.persistence.PermissionCheckRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository.RolePermissionRow;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.domain.authorization.Permission;
import com.example.demo.tenant.domain.authorization.TenantRole;

/** Roles are referenced by id from other aggregates, so they must be checked against the tenant. */
@Component
class TenantRoleChecks {
	private final TenantRoleRepository tenantRoleRepository;
	private final TenantRolePermissionRepository tenantRolePermissionRepository;
	private final PermissionCheckRepository permissionCheckRepository;

	TenantRoleChecks(TenantRoleRepository tenantRoleRepository,
			TenantRolePermissionRepository tenantRolePermissionRepository,
			PermissionCheckRepository permissionCheckRepository) {
		this.tenantRoleRepository = tenantRoleRepository;
		this.tenantRolePermissionRepository = tenantRolePermissionRepository;
		this.permissionCheckRepository = permissionCheckRepository;
	}

	/** Fails with a client error unless every id is an existing role of this tenant. */
	void requireRolesOfTenant(Collection<UUID> roleIds, UUID tenantId) {
		var ids = new HashSet<>(roleIds);
		if (!rolesOfTenant(ids, tenantId).containsAll(ids)) {
			throw new IllegalArgumentException("Some roles do not exist in this tenant");
		}
	}

	/** Like {@link #requireRolesOfTenant}, and the roles must also be active: inactive roles cannot be newly assigned. */
	void requireActiveRolesOfTenant(Collection<UUID> roleIds, UUID tenantId) {
		var ids = new HashSet<>(roleIds);
		var active = tenantRoleRepository.findAllById(ids).stream()
				.filter(role -> tenantId.equals(role.getTenantId()) && role.isActive())
				.map(TenantRole::getId)
				.collect(Collectors.toSet());
		if (!active.containsAll(ids)) {
			throw new IllegalArgumentException("Some roles do not exist or are inactive in this tenant");
		}
	}

	/** Nobody can hand out a role holding a permission they do not hold themselves. */
	void requireGrantable(Collection<UUID> roleIds, UUID actorId, UUID tenantId) {
		if (roleIds.isEmpty()) {
			return;
		}
		var missing = tenantRolePermissionRepository.findPermissionsByRoleIds(new HashSet<>(roleIds), tenantId).stream()
				.map(RolePermissionRow::permission)
				.map(Permission::getCode)
				.distinct()
				.sorted()
				.filter(code -> !permissionCheckRepository.hasPermission(actorId, tenantId, code))
				.toList();
		if (!missing.isEmpty()) {
			throw new AccessDeniedException("You cannot grant permissions you do not have: " + String.join(", ", missing));
		}
	}

	/** The subset of the ids that are existing (not deleted) roles of this tenant. */
	java.util.Set<UUID> rolesOfTenant(Collection<UUID> roleIds, UUID tenantId) {
		return tenantRoleRepository.findAllById(roleIds).stream()
				.filter(role -> tenantId.equals(role.getTenantId()))
				.map(TenantRole::getId)
				.collect(Collectors.toSet());
	}
}
