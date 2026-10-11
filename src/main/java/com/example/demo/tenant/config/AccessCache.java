package com.example.demo.tenant.config;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;

/**
 * Invalidation side of the {@link AccessSnapshot} cache. Every user has an epoch counter in Redis;
 * a snapshot is valid only while the epoch it was loaded under is still current. Invalidating is
 * just incrementing the counter, so it works for every session of the user at once.
 *
 * <p>Whatever changes a user's tenants, roles or permissions must call one of the
 * {@code invalidate*} methods. Inside a transaction the epochs move after the commit, so a reload
 * can never read the old data under the new epoch.
 *
 * <p>A global epoch is bumped on startup, which drops every snapshot after a deploy: migrations can
 * change permissions or grants without going through the services.
 */
@Component
public class AccessCache {
	private static final Logger log = LoggerFactory.getLogger(AccessCache.class);
	private static final String USER_KEY = "access:epoch:user:";
	private static final String GLOBAL_KEY = "access:epoch:global";

	private final StringRedisTemplate redis;
	private final TenantUserRepository tenantUserRepository;
	private final TenantUserRoleRepository tenantUserRoleRepository;

	public AccessCache(StringRedisTemplate redis, TenantUserRepository tenantUserRepository,
			TenantUserRoleRepository tenantUserRoleRepository) {
		this.redis = redis;
		this.tenantUserRepository = tenantUserRepository;
		this.tenantUserRoleRepository = tenantUserRoleRepository;
	}

	/** The user's epoch and the global epoch, in that order, read in one round trip. */
	long[] currentEpochs(UUID userId) {
		List<String> values = redis.opsForValue().multiGet(List.of(USER_KEY + userId, GLOBAL_KEY));
		return new long[] { epoch(USER_KEY + userId, values == null ? null : values.get(0)),
				epoch(GLOBAL_KEY, values == null ? null : values.get(1)) };
	}

	/** Membership, roles or role assignments of this user changed. */
	public void invalidateUser(UUID userId) {
		invalidateUsers(List.of(userId));
	}

	public void invalidateUsers(Collection<UUID> userIds) {
		var ids = List.copyOf(userIds);
		afterCommit(() -> ids.forEach(this::bump));
	}

	/** The tenant (for example its active state) changed, which affects every member. */
	public void invalidateTenant(UUID tenantId) {
		invalidateUsers(tenantUserRepository.findUserIdsByTenantId(tenantId));
	}

	/** The role's permissions or active state changed, which affects everyone holding it. */
	public void invalidateRole(UUID tenantId, UUID roleId) {
		invalidateUsers(tenantUserRoleRepository.findUserIdsByRole(tenantId, roleId));
	}

	@EventListener(ApplicationReadyEvent.class)
	void invalidateAllOnStartup() {
		redis.opsForValue().set(GLOBAL_KEY, String.valueOf(System.currentTimeMillis()));
	}

	/**
	 * A missing key (evicted or flushed Redis) is re-seeded with the current time, which is above any
	 * value handed out before, so a snapshot loaded under an earlier epoch can never match again.
	 */
	private long epoch(String key, String value) {
		if (value == null) {
			redis.opsForValue().setIfAbsent(key, String.valueOf(System.currentTimeMillis()));
			value = redis.opsForValue().get(key);
		}
		return Long.parseLong(value);
	}

	/** The change is already committed, so a Redis failure is logged; the snapshot TTL bounds the staleness. */
	private void bump(UUID userId) {
		try {
			redis.opsForValue().increment(USER_KEY + userId);
		} catch (RuntimeException e) {
			log.error("Could not invalidate cached access of user={}; it stays stale until its snapshot expires",
					userId, e);
		}
	}

	private static void afterCommit(Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}
}
