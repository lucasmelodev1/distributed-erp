package com.example.demo.tenant;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.CreateTenantResponse;
import com.example.demo.tenant.dto.UpdateTenantRequest;
import com.example.demo.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/tenant")
@Tag(name = "Tenant", description = "Management of the tenants, entities responsible for data isolation")
public class TenantController {
	private final TenantService tenantService;

	public TenantController(TenantService tenantService) {
		this.tenantService = tenantService;
	}

	@GetMapping
	@Operation(summary = "Find all user tenants", description = "Find all tenants that you are a member of")
	public List<Tenant> findAllUserTenants(@AuthenticationPrincipal User user) {
		return tenantService.findAllUserTenants(user.getId());
	}

	@PostMapping
	@Operation(summary = "Create tenant", description = "Create tenant and associate the current user to that tenant")
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

	@TenantAware(withPermissions = { "tenants:delete" })
	@DeleteMapping
	@Operation(summary = "Delete tenant")
	public void deleteTenant() {
		var tenantId = TenantContext.getTenantId();
		tenantService.deleteTenant(tenantId);
	}
}
