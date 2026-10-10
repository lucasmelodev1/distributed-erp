package com.example.demo.tenant.adapter.inbound;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.domain.authorization.TenantRole;

/** Roles are referenced by id from other aggregates, so they must be checked against the tenant. */
@Component
class TenantRoleChecks {
	private final TenantRoleRepository tenantRoleRepository;

	TenantRoleChecks(TenantRoleRepository tenantRoleRepository) {
		this.tenantRoleRepository = tenantRoleRepository;
	}

	/** Fails with a client error unless every id is an existing role of this tenant. */
	void requireRolesOfTenant(Collection<UUID> roleIds, UUID tenantId) {
		var ids = new HashSet<>(roleIds);
		if (!rolesOfTenant(ids, tenantId).containsAll(ids)) {
			throw new IllegalArgumentException("Some roles do not exist in this tenant");
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
