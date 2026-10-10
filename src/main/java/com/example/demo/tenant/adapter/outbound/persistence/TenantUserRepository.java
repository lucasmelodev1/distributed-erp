package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
	/** Whether a membership and its tenant are currently inactive; null timestamps mean active. */
	record MembershipState(@Nullable Instant memberInactivatedAt, @Nullable Instant tenantInactivatedAt) {
		public boolean memberActive() {
			return memberInactivatedAt == null;
		}

		public boolean tenantActive() {
			return tenantInactivatedAt == null;
		}
	}

	/** One query for both states; empty if the user is not a (non-removed) member of a (non-deleted) tenant. */
	@Query("""
			SELECT new com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository$MembershipState(
				tu.inactivatedAt, t.inactivatedAt)
			FROM TenantUser tu
			JOIN Tenant t ON t.id = tu.id.tenantId
			WHERE tu.id.tenantId = :tenantId
				AND tu.id.userId = :userId
			""")
	Optional<MembershipState> findMembershipState(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

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
