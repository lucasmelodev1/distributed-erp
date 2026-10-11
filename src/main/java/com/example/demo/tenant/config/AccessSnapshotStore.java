package com.example.demo.tenant.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.tenant.adapter.outbound.persistence.PermissionCheckRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.config.AccessSnapshot.TenantAccess;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Keeps the {@link AccessSnapshot} in the HTTP session and rebuilds it from the database when the
 * session has none, its epochs are stale or it is older than {@code app.access.snapshot-ttl}. The epochs are read before the database, so a change
 * committed meanwhile leaves the stored snapshot already outdated and the next request reloads it.
 */
@Component
public class AccessSnapshotStore {
	private static final String SESSION_ATTRIBUTE = "app.accessSnapshot";

	private final AccessCache accessCache;
	private final TenantUserRepository tenantUserRepository;
	private final PermissionCheckRepository permissionCheckRepository;

	private final Duration ttl;

	public AccessSnapshotStore(AccessCache accessCache, TenantUserRepository tenantUserRepository,
			PermissionCheckRepository permissionCheckRepository,
			@Value("${app.access.snapshot-ttl:5m}") Duration ttl) {
		this.ttl = ttl;
		this.accessCache = accessCache;
		this.tenantUserRepository = tenantUserRepository;
		this.permissionCheckRepository = permissionCheckRepository;
	}

	public AccessSnapshot snapshotFor(HttpServletRequest request, UUID userId) {
		long[] epochs = accessCache.currentEpochs(userId);
		var session = request.getSession();
		if (session.getAttribute(SESSION_ATTRIBUTE) instanceof AccessSnapshot stored
				&& stored.userId().equals(userId)
				&& stored.userEpoch() == epochs[0]
				&& stored.globalEpoch() == epochs[1]
				&& System.currentTimeMillis() - stored.loadedAtMillis() < ttl.toMillis()) {
			return stored;
		}
		var loaded = load(userId, epochs[0], epochs[1]);
		session.setAttribute(SESSION_ATTRIBUTE, loaded);
		return loaded;
	}

	AccessSnapshot load(UUID userId, long userEpoch, long globalEpoch) {
		Map<UUID, Set<String>> granted = new HashMap<>();
		for (var grant : permissionCheckRepository.findGrants(userId)) {
			granted.computeIfAbsent(grant.tenantId(), id -> new HashSet<>()).add(grant.permissionCode());
		}

		Map<UUID, TenantAccess> tenants = new HashMap<>();
		for (var membership : tenantUserRepository.findMemberships(userId)) {
			var permissions = granted.getOrDefault(membership.tenantId(), Set.of());
			tenants.put(membership.tenantId(), new TenantAccess(membership.memberInactivatedAt(),
					membership.tenantInactivatedAt(), Set.copyOf(permissions)));
		}
		return new AccessSnapshot(userId, userEpoch, globalEpoch, System.currentTimeMillis(), Map.copyOf(tenants));
	}
}
