package com.example.demo.tenant;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.dto.AcceptTenantInvitationRequest;
import com.example.demo.tenant.dto.CheckInvitedUserResponse;
import com.example.demo.tenant.dto.CheckInvitedUserResponse.CheckInvitedUserNextAction;
import com.example.demo.tenant.dto.InviteUserToTenantRequest;
import com.example.demo.tenant.dto.RevokeInvitationRequest;
import com.example.demo.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tenant-user")
@Tag(name = "Tenant User", description = "Endpoints for managing tenant users and invites")
public class TenantUserController {
    private final TenantUserService tenantService;

    public TenantUserController(TenantUserService tenantUserService) {
        this.tenantService = tenantUserService;
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
