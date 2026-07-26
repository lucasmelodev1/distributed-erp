package com.example.demo.tenant.authorization;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionCheckRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {

	@Query(value = """
			SELECT EXISTS(
				SELECT 1
				FROM tenant_user_roles tur
				JOIN tenant_role_permissions trp ON trp.tenant_role_id = tur.tenant_role_id
				WHERE tur.user_id = :userId
					AND tur.tenant_id = :tenantId
					AND trp.permission_code = :permissionCode
			)
			""", nativeQuery = true)
	boolean hasPermission(
			@Param("userId") UUID userId,
			@Param("tenantId") UUID tenantId,
			@Param("permissionCode") String permissionCode);
}
