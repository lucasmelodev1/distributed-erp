package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUserRepository
		extends
		JpaRepository<TenantUser, TenantUserId>,
		JpaSpecificationExecutor<TenantUser> {
	/** A membership of one user and the inactive state of it and of its tenant; null timestamps mean active. */
	record MembershipRow(UUID tenantId, @Nullable Instant memberInactivatedAt, @Nullable Instant tenantInactivatedAt) {
	}

	/** All (non-removed) memberships of a user in non-deleted tenants, in one query. */
	@Query("""
			SELECT new com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository$MembershipRow(
				tu.id.tenantId, tu.inactivatedAt, t.inactivatedAt)
			FROM TenantUser tu
			JOIN Tenant t ON t.id = tu.id.tenantId
			WHERE tu.id.userId = :userId
			""")
	List<MembershipRow> findMemberships(@Param("userId") UUID userId);

	@Query("SELECT tu.id.userId FROM TenantUser tu WHERE tu.id.tenantId = :tenantId")
	List<UUID> findUserIdsByTenantId(@Param("tenantId") UUID tenantId);

	/**
	 * Brings back a membership that was removed (soft-deleted). Native because the
	 * soft-delete filter hides such rows from JPQL. Returns how many rows were restored.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = """
			UPDATE tenant_users
			SET deleted_at = NULL, inactivated_at = NULL, updated_at = CURRENT_TIMESTAMP(6)
			WHERE tenant_id = :tenantId
				AND user_id = :userId
				AND deleted_at IS NOT NULL
			""", nativeQuery = true)
	int restoreRemoved(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

	@Query(value = """
			SELECT tu FROM User u
			JOIN TenantUser tu ON tu.id.userId = u.id
			WHERE u.email = :email
				AND tu.id.tenantId = :tenantId
				""")
	List<TenantUser> findByUserEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);
}
