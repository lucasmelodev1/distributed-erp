package com.example.demo.tenant.adapter.web;

import java.util.Optional;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.port.inbound.TenantUserManagementPort;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.adapter.web.dto.user.AcceptTenantInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.CheckInvitedUserResponse;
import com.example.demo.tenant.adapter.web.dto.user.InactivateTenantUserRequest;
import com.example.demo.tenant.adapter.web.dto.user.CheckInvitedUserResponse.CheckInvitedUserNextAction;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantRequest;
import com.example.demo.tenant.adapter.web.dto.user.RevokeInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserRequest;
import com.example.demo.user.domain.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/tenant-user")
@Tag(name = "Tenant User", description = "Endpoints for managing tenant users and invites")
public class TenantUserController {
    private final TenantUserManagementPort tenantUserService;

    public TenantUserController(TenantUserManagementPort tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @TenantAware(withPermissions = "tenants:invite")
    @PostMapping("/invite")
    @Operation(summary = "Invite user to tenant", description = "Invite user to tenant and sends them an email")
    public void inviteUserToTenant(
            @Valid @RequestBody InviteUserToTenantRequest dto,
            @AuthenticationPrincipal User user) {
        var tenantId = TenantContext.getTenantId();
        tenantUserService.inviteUserToTenant(dto, tenantId, user);
    }

    @PostMapping("/invite/check-user")
    @Operation(summary = "Check invited user", description = "Check if invited user is logged in, if they exist or if they need to create an account")
    public CheckInvitedUserResponse checkInvitedUser(@RequestParam UUID code, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            tenantUserService.acceptInvitation(new AcceptTenantInvitationRequest(code), user);
            return new CheckInvitedUserResponse(CheckInvitedUserNextAction.ACCEPT);
        } else {
            return tenantUserService.checkInvitedUser(code);
        }
    }

    @TenantAware(withPermissions = "tenants:invite")
    @PostMapping("/revoke-invite")
    @Operation(summary = "Revoke invitation")
    public void revokeInvite(@RequestBody RevokeInvitationRequest dto) {
        var tenantId = TenantContext.getTenantId();
        tenantUserService.revokeInvitation(dto, tenantId);
    }

    @TenantAware(withPermissions = "tenant_users:read")
    @GetMapping("/{userId}")
    @Operation(summary = "Get tenant user", description = "Get a tenant user by their user ID")
    public Optional<TenantUser> findTenantUserById(@PathVariable UUID userId) {
        var tenantId = TenantContext.getTenantId();
        return tenantUserService.findTenantUserById(userId, tenantId);
    }

    @TenantAware(withPermissions = "tenant_users:read")
    @GetMapping
    @Operation(summary = "Get tenant users", description = "Get all users for the current tenant")
    public Page<TenantUser> findAllTenantUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault Pageable pageable) {
        var tenantId = TenantContext.getTenantId();
        return tenantUserService.findAllTenantUsers(query, active, pageable, tenantId);
    }

    @TenantAware(withPermissions = "tenant_users:inactivate")
    @PostMapping("/{userId}/inactivate")
    @Operation(summary = "Inactivate tenant user", description = "Inactivate a tenant user")
    public void inactivateTenantUser(
            @PathVariable UUID userId,
            @Valid @RequestBody InactivateTenantUserRequest dto) {
        var tenantId = TenantContext.getTenantId();
        tenantUserService.updateTenantUserActiveStatus(userId, dto, tenantId);
    }

    @TenantAware(withPermissions = "tenant_users:update")
    @PutMapping("/{userId}")
    @Operation(summary = "Update tenant user", description = "Update a tenant user")
    public void updateTenantUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTenantUserRequest dto) {
        var tenantId = TenantContext.getTenantId();
        tenantUserService.updateTenantUser(userId, dto, tenantId);
    }
}
