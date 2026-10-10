package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.invitation.TenantInvitationRoleId;
import com.example.demo.tenant.domain.invitation.TenantInvitationRole;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.tenant.domain.authorization.TenantRole;

public interface TenantInvitationRoleRepository extends JpaRepository<TenantInvitationRole, TenantInvitationRoleId> {

	List<TenantInvitationRole> findByIdTenantInvitationId(UUID tenantInvitationId);

	record InvitationRoleRow(UUID invitationId, TenantRole role) {
	}

	/** Roles of several invitations of one tenant in a single query. */
	@Query("""
			SELECT new com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRoleRepository$InvitationRoleRow(
				tir.id.tenantInvitationId, r)
			FROM TenantInvitationRole tir
			JOIN TenantRole r ON r.id = tir.id.roleId
			WHERE tir.id.tenantInvitationId IN :invitationIds
				AND r.tenantId = :tenantId
			ORDER BY r.id
			""")
	List<InvitationRoleRow> findRolesByInvitationIds(@Param("invitationIds") Collection<UUID> invitationIds,
			@Param("tenantId") UUID tenantId);
}
