package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.tenant.domain.authorization.TenantRole;

public interface TenantUserRoleRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {

	record UserRoleRow(UUID userId, TenantRole role) {
	}

	@Query("""
			SELECT tur.id.userId FROM TenantUserRole tur
			WHERE tur.id.tenantId = :tenantId AND tur.id.tenantRoleId = :roleId
			""")
	List<UUID> findUserIdsByRole(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

	/** Roles of several users of one tenant in a single query. */
	@Query("""
			SELECT new com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository$UserRoleRow(
				tur.id.userId, r)
			FROM TenantUserRole tur
			JOIN TenantRole r ON r.id = tur.id.tenantRoleId
			WHERE tur.id.tenantId = :tenantId
				AND r.tenantId = :tenantId
				AND tur.id.userId IN :userIds
			ORDER BY r.id
			""")
	List<UserRoleRow> findRolesByUserIds(@Param("userIds") Collection<UUID> userIds,
			@Param("tenantId") UUID tenantId);
}
