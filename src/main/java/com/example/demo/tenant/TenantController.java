package com.example.demo.tenant;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.dto.AcceptTenantInvitationRequest;
import com.example.demo.tenant.dto.CheckInvitedUserResponse;
import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.CreateTenantResponse;
import com.example.demo.tenant.dto.InviteUserToTenantRequest;
import com.example.demo.tenant.dto.RevokeInvitationRequest;
import com.example.demo.tenant.dto.UpdateTenantRequest;
import com.example.demo.tenant.dto.CheckInvitedUserResponse.CheckInvitedUserNextAction;
import com.example.demo.user.User;

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

	@TenantAware(withPermissions = "tenants:delete")
	@DeleteMapping
	@Operation(summary = "Delete tenant")
	public void deleteTenant() {
		var tenantId = TenantContext.getTenantId();
		tenantService.deleteTenant(tenantId);
	}

	@TenantAware(withPermissions = "tenants:invite")
	@PostMapping("/invite")
	@Operation(summary = "Invite user to tenant", description = "Invite user to tenant and sends them an email")
	public void inviteUserToTenant(
			@Valid @RequestBody InviteUserToTenantRequest dto,
			@AuthenticationPrincipal User user) {
		var tenantId = TenantContext.getTenantId();
		tenantService.inviteUserToTenant(dto, tenantId, user);
	}

	@PostMapping("/invite/check-user")
	@Operation(summary = "Check invited user", description = "Check if invited user is logged in, if they exist or if they need to create an account")
	public CheckInvitedUserResponse checkInvitedUser(@RequestParam UUID code, Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof User user) {
			tenantService.acceptInvitation(new AcceptTenantInvitationRequest(code), user);
			return new CheckInvitedUserResponse(CheckInvitedUserNextAction.ACCEPT);
		} else {
			return tenantService.checkInvitedUser(code);
		}
	}

	@TenantAware(withPermissions = "tenants:invite")
	@PostMapping("/revoke-invite")
	@Operation(summary = "Revoke invitation")
	public void revokeInvite(@RequestBody RevokeInvitationRequest dto) {
		var tenantId = TenantContext.getTenantId();
		tenantService.revokeInvitation(dto, tenantId);
	}

}
