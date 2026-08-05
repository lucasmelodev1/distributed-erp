package com.example.demo.tenant.adapter.web;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.port.inbound.TenantManagementPort;
import com.example.demo.tenant.domain.Tenant;
import com.example.demo.user.domain.User;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantRequest;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantResponse;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/tenant")
@Tag(name = "Tenant", description = "Management of the tenants, entities responsible for data isolation")
public class TenantController {
	private final TenantManagementPort tenantService;

	public TenantController(TenantManagementPort tenantService) {
		this.tenantService = tenantService;
	}

	@GetMapping
	@Operation(summary = "Find all user tenants", description = "Find all tenants that you are a member of")
	public List<Tenant> findAllUserTenants(@AuthenticationPrincipal User user) {
		return tenantService.findAllUserTenants(user.getId());
	}

	@TenantAware
	@GetMapping("/current")
	@Operation(summary = "Find current tenant", description = "Find the current tenant. The authenticated user must be a member of this tenant")
	public Tenant findTenantById() {
		var tenantId = TenantContext.getTenantId();
		return tenantService.findTenantById(tenantId)
				.orElseThrow(() -> new DataIntegrityViolationException("Current tenant should NEVER be null"));
	}

	@PostMapping
	@Operation(summary = "Create tenant", description = "Create tenant and associate the current user to that tenant")
	@ResponseStatus(code = HttpStatus.CREATED)
	public CreateTenantResponse createTenant(
			@Valid @RequestBody CreateTenantRequest dto,
			@AuthenticationPrincipal User user) {
		return tenantService.createTenant(dto, user.getId());
	}

	@TenantAware(withPermissions = { "tenants:update" })
	@PutMapping
	@Operation(summary = "Update tenant")
	public void updateTenant(@Valid @RequestBody UpdateTenantRequest dto) {
		var tenantId = TenantContext.getTenantId();
		tenantService.updateTenant(dto, tenantId);
	}

	@TenantAware(withPermissions = "tenants:delete")
	@DeleteMapping
	@Operation(summary = "Delete tenant")
	public void deleteTenant() {
		var tenantId = TenantContext.getTenantId();
		tenantService.deleteTenant(tenantId);
	}
}
