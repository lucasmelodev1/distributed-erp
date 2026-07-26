package com.example.demo.tenant.invitation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, UUID> {

	@Query("""
			SELECT ti FROM TenantInvitation ti
			WHERE ti.id = :id
				AND ti.email = :email
				AND ti.consumedAt IS NULL
				AND ti.revokedAt IS NULL
				AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)
			""")
	List<TenantInvitation> findActiveByEmailAndId(
			@Param("email") String email,
			@Param("id") UUID id);

	@Query("""
			SELECT ti FROM TenantInvitation ti
			WHERE ti.tenantId = :tenantId
				AND ti.email = :email
				AND ti.consumedAt IS NULL
				AND ti.revokedAt IS NULL
				AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)
			""")
	List<TenantInvitation> findActiveByEmailAndTenantId(
			@Param("email") String email,
			@Param("tenantId") UUID tenantId);

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

	@Query(value = """
			UPDATE TenantInvitation ti
			SET ti.revokedAt = CURRENT_TIMESTAMP
			WHERE ti.id = :id
				AND ti.tenantId = :tenantId
				AND ti.consumedAt IS NULL
				AND ti.revokedAt IS NULL
				AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)
				""")
	void revokeByIdAndTenantId(@Param("id") UUID invitationId, @Param("tenantId") UUID tenantId);
}
