package com.example.demo.tenant.adapter.outbound.persistence;

import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionCheckRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {

	@Query(value = """
			SELECT COUNT(*)
			FROM tenant_user_roles tur
			JOIN tenant_role_permissions trp ON trp.tenant_role_id = tur.tenant_role_id
			WHERE tur.user_id = :userId
				AND tur.tenant_id = :tenantId
				AND trp.permission_code = :permissionCode
			""", nativeQuery = true)
	long countPermissionGrants(
			@Param("userId") UUID userId,
			@Param("tenantId") UUID tenantId,
			@Param("permissionCode") String permissionCode);

	default boolean hasPermission(UUID userId, UUID tenantId, String permissionCode) {
		return countPermissionGrants(userId, tenantId, permissionCode) > 0;
	}
}
