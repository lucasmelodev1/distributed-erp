package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.authorization.TenantRolePermission;
import com.example.demo.tenant.domain.authorization.TenantRolePermissionId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.tenant.domain.authorization.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRolePermissionRepository
		extends JpaRepository<TenantRolePermission, TenantRolePermissionId> {

	record RolePermissionRow(UUID roleId, Permission permission) {
	}

	/** Permissions of several roles in one query, without loading the roles themselves. */
	@Query("""
			SELECT new com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository$RolePermissionRow(
				trp.id.tenantRoleId, p)
			FROM TenantRolePermission trp
			JOIN Permission p ON p.code = trp.id.permissionCode
			WHERE trp.id.tenantRoleId IN :roleIds
				AND trp.tenantId = :tenantId
			ORDER BY p.code
			""")
	List<RolePermissionRow> findPermissionsByRoleIds(@Param("roleIds") Collection<UUID> roleIds,
			@Param("tenantId") UUID tenantId);
}
