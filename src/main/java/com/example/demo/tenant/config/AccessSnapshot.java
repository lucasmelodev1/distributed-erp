package com.example.demo.tenant.config;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Everything the access checks need to know about one user: their tenants, the state of each
 * membership and tenant, and the permissions granted by their active roles. It is kept in the
 * session (Redis) so permission checks do not hit the database on every request.
 *
 * <p>{@code userEpoch} and {@code globalEpoch} are the {@link AccessCache} epochs read before the
 * snapshot was loaded; once either moves on, the snapshot is stale and is rebuilt.
 * {@code loadedAtMillis} bounds how long a snapshot lives even if an invalidation was lost.
 */
public record AccessSnapshot(UUID userId, long userEpoch, long globalEpoch, long loadedAtMillis,
		Map<UUID, TenantAccess> tenants) implements Serializable {

	/**
	 * The user's standing in one tenant. {@code permissions} only holds grants from active roles;
	 * whether the membership and the tenant are active is decided separately.
	 */
	public record TenantAccess(@Nullable Instant memberInactivatedAt, @Nullable Instant tenantInactivatedAt,
			Set<String> permissions) implements Serializable {

		public boolean memberActive() {
			return memberInactivatedAt == null;
		}

		public boolean tenantActive() {
			return tenantInactivatedAt == null;
		}
	}
}
