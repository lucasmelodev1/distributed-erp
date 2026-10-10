package com.example.demo.tenant.adapter.web.graphql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.dataloader.MappedBatchLoader;

import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.port.inbound.TenantUserManagementPort;
import com.netflix.graphql.dgs.DgsDataLoader;

/** Loads the roles of many members of a tenant with one query. The tenant is part of the key and the query. */
@DgsDataLoader(name = TenantUserRolesLoader.NAME)
public class TenantUserRolesLoader implements MappedBatchLoader<TenantUserRolesLoader.Key, List<TenantRole>> {
	public static final String NAME = "tenantUserRoles";

	public record Key(UUID userId, UUID tenantId) {
	}

	private final TenantUserManagementPort tenantUserService;

	public TenantUserRolesLoader(TenantUserManagementPort tenantUserService) {
		this.tenantUserService = tenantUserService;
	}

	@Override
	public CompletionStage<Map<Key, List<TenantRole>>> load(Set<Key> keys) {
		Map<Key, List<TenantRole>> result = new HashMap<>();
		keys.stream().collect(Collectors.groupingBy(Key::tenantId)).forEach((tenantId, tenantKeys) -> {
			var userIds = tenantKeys.stream().map(Key::userId).collect(Collectors.toSet());
			var rolesByUser = tenantUserService.findRolesByUserIds(userIds, tenantId);
			tenantKeys.forEach(key -> result.put(key, rolesByUser.getOrDefault(key.userId(), List.of())));
		});
		return CompletableFuture.completedFuture(result);
	}
}
