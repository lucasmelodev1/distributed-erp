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
import com.example.demo.tenant.port.inbound.TenantInvitationPort;
import com.netflix.graphql.dgs.DgsDataLoader;

/** Loads the roles of many invitations of a tenant with one query. The tenant is part of the key and the query. */
@DgsDataLoader(name = TenantInvitationRolesLoader.NAME)
public class TenantInvitationRolesLoader
		implements MappedBatchLoader<TenantInvitationRolesLoader.Key, List<TenantRole>> {
	public static final String NAME = "tenantInvitationRoles";

	public record Key(UUID invitationId, UUID tenantId) {
	}

	private final TenantInvitationPort invitationService;

	public TenantInvitationRolesLoader(TenantInvitationPort invitationService) {
		this.invitationService = invitationService;
	}

	@Override
	public CompletionStage<Map<Key, List<TenantRole>>> load(Set<Key> keys) {
		Map<Key, List<TenantRole>> result = new HashMap<>();
		keys.stream().collect(Collectors.groupingBy(Key::tenantId)).forEach((tenantId, tenantKeys) -> {
			var ids = tenantKeys.stream().map(Key::invitationId).collect(Collectors.toSet());
			var rolesByInvitation = invitationService.findRolesByInvitationIds(ids, tenantId);
			tenantKeys.forEach(key -> result.put(key, rolesByInvitation.getOrDefault(key.invitationId(), List.of())));
		});
		return CompletableFuture.completedFuture(result);
	}
}
