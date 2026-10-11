package com.example.demo.tenant.domain.invitation;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import com.example.demo.tenant.domain.invitation.TenantInvitation.Status;

public class TenantInvitationSpecification {

	public static Specification<TenantInvitation> hasTenantId(UUID tenantId) {
		return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
	}

	public static Specification<TenantInvitation> hasEmailContaining(@Nullable String text) {
		return (root, query, cb) -> text == null || text.isBlank() ? null
				: cb.like(root.get("email"), "%" + text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%", '\\');
	}

	/** Mirrors {@link TenantInvitation#getStatus()}: accepted wins over revoked, which wins over expired. */
	public static Specification<TenantInvitation> hasStatus(@Nullable Status status, Instant now) {
		return (root, query, cb) -> {
			if (status == null) {
				return null;
			}
			var consumed = root.get("consumedAt");
			var revoked = root.get("revokedAt");
			var expires = root.<Instant>get("expiresAt");
			var notExpired = cb.or(cb.isNull(expires), cb.greaterThanOrEqualTo(expires, now));
			return switch (status) {
				case ACCEPTED -> cb.isNotNull(consumed);
				case REVOKED -> cb.and(cb.isNull(consumed), cb.isNotNull(revoked));
				case EXPIRED -> cb.and(cb.isNull(consumed), cb.isNull(revoked), cb.not(notExpired));
				case PENDING -> cb.and(cb.isNull(consumed), cb.isNull(revoked), notExpired);
			};
		};
	}
}
