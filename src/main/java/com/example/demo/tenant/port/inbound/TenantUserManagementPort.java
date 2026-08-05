package com.example.demo.tenant.port.inbound;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.tenant.adapter.web.dto.user.AcceptTenantInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.CheckInvitedUserResponse;
import com.example.demo.tenant.adapter.web.dto.user.InactivateTenantUserRequest;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantRequest;
import com.example.demo.tenant.adapter.web.dto.user.RevokeInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserRequest;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.user.domain.User;

public interface TenantUserManagementPort {
    void inviteUserToTenant(InviteUserToTenantRequest dto, UUID tenantId, User user);
    void acceptInvitation(AcceptTenantInvitationRequest dto, User user);
    CheckInvitedUserResponse checkInvitedUser(UUID invitationId);
    void revokeInvitation(RevokeInvitationRequest dto, UUID tenantId);
    Page<TenantUser> findAllTenantUsers(@Nullable String query, @Nullable Boolean active, Pageable pageable, UUID tenantId);
    Optional<TenantUser> findTenantUserById(UUID userId, UUID tenantId);
    void updateTenantUserActiveStatus(UUID userId, InactivateTenantUserRequest dto, UUID tenantId);
    void updateTenantUser(UUID userId, UpdateTenantUserRequest dto, UUID tenantId);
}
