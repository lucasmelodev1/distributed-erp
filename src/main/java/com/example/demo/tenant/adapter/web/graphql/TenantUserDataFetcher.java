package com.example.demo.tenant.adapter.web.graphql;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import com.example.demo.config.graphql.Connection;
import com.example.demo.config.graphql.Cursors;
import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.tenant.adapter.web.dto.user.TenantUserIdInput;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserInput;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.port.inbound.TenantUserManagementPort;
import com.example.demo.user.adapter.web.dto.user.UserProfile;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import jakarta.persistence.EntityNotFoundException;

@DgsComponent
public class TenantUserDataFetcher {
	private final TenantUserManagementPort tenantUserService;
	private final GraphQlInputValidator validator;

	public TenantUserDataFetcher(TenantUserManagementPort tenantUserService, GraphQlInputValidator validator) {
		this.tenantUserService = tenantUserService;
		this.validator = validator;
	}

	@TenantAware(withPermissions = "tenant_users:read")
	@DgsQuery
	public Connection<TenantUser> tenantUsers(@InputArgument Integer first, @InputArgument String after,
			@InputArgument String query, @InputArgument Boolean active) {
		var window = tenantUserService.scrollTenantUsers(query, active, Cursors.pageSize(first),
				Cursors.decode(after), TenantContext.getTenantId());
		return Cursors.toConnection(window, tenantUser -> tenantUser.getId().userId(), after);
	}

	@TenantAware(withPermissions = "tenant_users:read")
	@DgsQuery
	public TenantUser tenantUser(@InputArgument UUID userId) {
		return tenantUserService.findTenantUserById(userId, TenantContext.getTenantId()).orElse(null);
	}

	@DgsData(parentType = "TenantUser", field = "user")
	public CompletableFuture<UserProfile> user(DgsDataFetchingEnvironment dfe) {
		TenantUser tenantUser = dfe.getSource();
		DataLoader<UUID, UserProfile> loader = dfe.getDataLoader(UserProfileLoader.NAME);
		return loader.load(tenantUser.getId().userId());
	}

	@DgsData(parentType = "TenantUser", field = "roles")
	public CompletableFuture<List<TenantRole>> roles(DgsDataFetchingEnvironment dfe) {
		TenantUser tenantUser = dfe.getSource();
		DataLoader<TenantUserRolesLoader.Key, List<TenantRole>> loader = dfe
				.getDataLoader(TenantUserRolesLoader.NAME);
		return loader.load(new TenantUserRolesLoader.Key(tenantUser.getId().userId(), tenantUser.getId().tenantId()));
	}

	@TenantAware(withPermissions = "tenant_users:update")
	@DgsMutation
	public Map<String, Object> updateTenantUser(@InputArgument UpdateTenantUserInput input,
			DgsDataFetchingEnvironment dfe) {
		var tenantId = TenantContext.getTenantId();
		tenantUserService.updateTenantUser(GraphQlRequestContext.currentUser(dfe).getId(), validator.validate(input),
				tenantId);
		return Map.of("tenantUser", requireTenantUser(input.userId(), tenantId));
	}

	@TenantAware(withPermissions = "tenant_users:inactivate")
	@DgsMutation
	public Map<String, Object> inactivateTenantUser(@InputArgument TenantUserIdInput input,
			DgsDataFetchingEnvironment dfe) {
		return setActive(input, false, dfe);
	}

	@TenantAware(withPermissions = "tenant_users:inactivate")
	@DgsMutation
	public Map<String, Object> reactivateTenantUser(@InputArgument TenantUserIdInput input,
			DgsDataFetchingEnvironment dfe) {
		return setActive(input, true, dfe);
	}

	@TenantAware(withPermissions = "tenants:expel")
	@DgsMutation
	public Map<String, Object> removeTenantUser(@InputArgument TenantUserIdInput input,
			DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		tenantUserService.removeTenantUser(GraphQlRequestContext.currentUser(dfe).getId(), input.userId(),
				TenantContext.getTenantId());
		return Map.of("removedUserId", input.userId());
	}

	private Map<String, Object> setActive(TenantUserIdInput input, boolean active, DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		var tenantId = TenantContext.getTenantId();
		tenantUserService.updateTenantUserActiveStatus(GraphQlRequestContext.currentUser(dfe).getId(),
				input.userId(), active, tenantId);
		return Map.of("tenantUser", requireTenantUser(input.userId(), tenantId));
	}

	private TenantUser requireTenantUser(UUID userId, UUID tenantId) {
		return tenantUserService.findTenantUserById(userId, tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Tenant user not found"));
	}
}
