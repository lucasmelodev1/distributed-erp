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

import com.example.demo.tenant.domain.authorization.Permission;
import com.example.demo.tenant.port.inbound.TenantRoleManagementPort;
import com.netflix.graphql.dgs.DgsDataLoader;

/**
 * Loads the permissions of many roles with a single query so that listing roles
 * does not trigger one query per role. The tenant is part of the key and of the
 * query, so a batch can never read another tenant's rows.
 */
@DgsDataLoader(name = TenantRolePermissionsLoader.NAME)
public class TenantRolePermissionsLoader implements MappedBatchLoader<TenantRolePermissionsLoader.Key, List<Permission>> {
	public static final String NAME = "tenantRolePermissions";

	public record Key(UUID roleId, UUID tenantId) {
	}

	private final TenantRoleManagementPort tenantRoleService;

	public TenantRolePermissionsLoader(TenantRoleManagementPort tenantRoleService) {
		this.tenantRoleService = tenantRoleService;
	}

	@Override
	public CompletionStage<Map<Key, List<Permission>>> load(Set<Key> keys) {
		Map<Key, List<Permission>> result = new HashMap<>();
		keys.stream().collect(Collectors.groupingBy(Key::tenantId)).forEach((tenantId, tenantKeys) -> {
			var roleIds = tenantKeys.stream().map(Key::roleId).collect(Collectors.toSet());
			var permissionsByRole = tenantRoleService.findPermissionsByRoleIds(roleIds, tenantId);
			tenantKeys.forEach(key -> result.put(key, permissionsByRole.getOrDefault(key.roleId(), List.of())));
		});
		return CompletableFuture.completedFuture(result);
	}
}
