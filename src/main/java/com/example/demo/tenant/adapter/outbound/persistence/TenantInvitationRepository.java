package com.example.demo.tenant.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.tenant.domain.invitation.TenantInvitation;

public interface TenantInvitationRepository
		extends JpaRepository<TenantInvitation, UUID>, JpaSpecificationExecutor<TenantInvitation> {

	Optional<TenantInvitation> findByCodeHash(String codeHash);

	Optional<TenantInvitation> findByIdAndTenantId(UUID id, UUID tenantId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE TenantInvitation ti
			SET ti.revokedAt = CURRENT_TIMESTAMP
			WHERE ti.tenantId = :tenantId
				AND ti.email = :email
				AND ti.consumedAt IS NULL
				AND ti.revokedAt IS NULL
				AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)
			""")
	void revokeAllActiveByTenantIdAndUserEmail(
			@Param("tenantId") UUID tenantId,
			@Param("email") String email);
}
