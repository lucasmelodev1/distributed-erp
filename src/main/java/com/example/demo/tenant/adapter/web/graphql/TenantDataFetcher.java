package com.example.demo.tenant.adapter.web.graphql;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantInput;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantInput;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.Tenant;
import com.example.demo.tenant.port.inbound.TenantManagementPort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class TenantDataFetcher {
	private final TenantManagementPort tenantService;
	private final GraphQlInputValidator validator;

	public TenantDataFetcher(TenantManagementPort tenantService, GraphQlInputValidator validator) {
		this.tenantService = tenantService;
		this.validator = validator;
	}

	@DgsQuery
	public List<Tenant> myTenants(DgsDataFetchingEnvironment dfe) {
		return tenantService.findAllUserTenants(GraphQlRequestContext.currentUser(dfe).getId());
	}

	@TenantAware
	@DgsQuery
	public Tenant currentTenant() {
		return tenantService.findTenantById(TenantContext.getTenantId())
				.orElseThrow(() -> new DataIntegrityViolationException("Current tenant should NEVER be null"));
	}

	@DgsMutation
	public Map<String, Object> createTenant(@InputArgument CreateTenantInput input,
			DgsDataFetchingEnvironment dfe) {
		return Map.of("tenant", tenantService.createTenant(validator.validate(input),
				GraphQlRequestContext.currentUser(dfe).getId()));
	}

	@TenantAware(withPermissions = "tenants:update")
	@DgsMutation
	public Map<String, Object> updateTenant(@InputArgument UpdateTenantInput input) {
		var tenant = tenantService.updateTenant(validator.validate(input), TenantContext.getTenantId());
		return Map.of("tenant", tenant);
	}

	@TenantAware(withPermissions = "tenants:inactivate")
	@DgsMutation
	public Map<String, Object> inactivateTenant() {
		return Map.of("tenant", tenantService.setTenantActive(TenantContext.getTenantId(), false));
	}

	@TenantAware(withPermissions = "tenants:inactivate", allowInactiveTenant = true)
	@DgsMutation
	public Map<String, Object> reactivateTenant() {
		return Map.of("tenant", tenantService.setTenantActive(TenantContext.getTenantId(), true));
	}
}
