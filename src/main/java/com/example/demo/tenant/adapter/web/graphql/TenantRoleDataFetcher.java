package com.example.demo.tenant.adapter.web.graphql;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import com.example.demo.config.graphql.Connection;
import com.example.demo.config.graphql.Cursors;
import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleInput;
import com.example.demo.tenant.adapter.web.dto.role.TenantRoleIdInput;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleInput;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.authorization.Permission;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.port.inbound.TenantRoleManagementPort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class TenantRoleDataFetcher {
	private final TenantRoleManagementPort tenantRoleService;
	private final GraphQlInputValidator validator;

	public TenantRoleDataFetcher(TenantRoleManagementPort tenantRoleService, GraphQlInputValidator validator) {
		this.tenantRoleService = tenantRoleService;
		this.validator = validator;
	}

	@TenantAware(withPermissions = "tenant_roles:read")
	@DgsQuery
	public Connection<TenantRole> tenantRoles(@InputArgument Integer first, @InputArgument String after,
			@InputArgument String query, @InputArgument Boolean active) {
		var window = tenantRoleService.scrollTenantRoles(query, active, Cursors.pageSize(first), Cursors.decode(after),
				TenantContext.getTenantId());
		return Cursors.toConnection(window, TenantRole::getId, after);
	}

	@TenantAware(withPermissions = "tenant_roles:read")
	@DgsQuery
	public List<Permission> permissions() {
		return tenantRoleService.findAllPermissions();
	}

	@TenantAware(withPermissions = "tenant_roles:read")
	@DgsQuery
	public TenantRole tenantRole(@InputArgument UUID id) {
		return tenantRoleService.findTenantRoleById(id, TenantContext.getTenantId());
	}

	@DgsData(parentType = "TenantRole", field = "permissions")
	public CompletableFuture<List<Permission>> permissions(DgsDataFetchingEnvironment dfe) {
		TenantRole role = dfe.getSource();
		DataLoader<TenantRolePermissionsLoader.Key, List<Permission>> loader = dfe
				.getDataLoader(TenantRolePermissionsLoader.NAME);
		return loader.load(new TenantRolePermissionsLoader.Key(role.getId(), role.getTenantId()));
	}

	@TenantAware(withPermissions = "tenant_roles:create")
	@DgsMutation
	public Map<String, Object> createTenantRole(@InputArgument CreateTenantRoleInput input) {
		return Map.of("tenantRole",
				tenantRoleService.createTenantRole(validator.validate(input), TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "tenant_roles:update")
	@DgsMutation
	public Map<String, Object> updateTenantRole(@InputArgument UpdateTenantRoleInput input) {
		return Map.of("tenantRole",
				tenantRoleService.updateTenantRole(validator.validate(input), TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "tenant_roles:inactivate")
	@DgsMutation
	public Map<String, Object> inactivateTenantRole(@InputArgument TenantRoleIdInput input) {
		validator.validate(input);
		return Map.of("tenantRole",
				tenantRoleService.setTenantRoleActive(input.id(), false, TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "tenant_roles:inactivate")
	@DgsMutation
	public Map<String, Object> reactivateTenantRole(@InputArgument TenantRoleIdInput input) {
		validator.validate(input);
		return Map.of("tenantRole",
				tenantRoleService.setTenantRoleActive(input.id(), true, TenantContext.getTenantId()));
	}
}
