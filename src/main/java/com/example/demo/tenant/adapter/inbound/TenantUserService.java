package com.example.demo.tenant.adapter.inbound;

import com.example.demo.tenant.port.inbound.TenantUserManagementPort;

import com.example.demo.email.EmailService;
import com.example.demo.utils.EnvUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.adapter.outbound.persistence.PermissionCheckRepository;
import com.example.demo.tenant.adapter.outbound.persistence.PermissionRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.adapter.web.dto.user.AcceptTenantInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.CheckInvitedUserResponse;
import com.example.demo.tenant.adapter.web.dto.user.InactivateTenantUserRequest;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantRequest;
import com.example.demo.tenant.adapter.web.dto.user.RevokeInvitationRequest;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserRequest;
import com.example.demo.tenant.adapter.web.dto.user.CheckInvitedUserResponse.CheckInvitedUserNextAction;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.TenantUserSpecification;
import com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRoleRepository;
import com.example.demo.user.domain.User;
import com.example.demo.user.adapter.outbound.persistence.UserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

@Service
public class TenantUserService implements TenantUserManagementPort {
    private final EnvUtils envUtils;
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantInvitationRepository tenantInvitationRepository;
    private final TenantInvitationRoleRepository tenantInvitationRoleRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${TENANT_INVITATION_FROM:invitations@example.com}")
    private String invitationFrom;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    TenantUserService(
            TenantRepository tenantRepository,
            TenantUserRepository tenantUserRepository,
            TenantRoleRepository tenantRoleRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantRolePermissionRepository tenantRolePermissionRepository,
            PermissionCheckRepository permissionCheckRepository,
            PermissionRepository permissionRepository,
            TenantInvitationRepository tenantInvitationRepository,
            TenantInvitationRoleRepository tenantInvitationRoleRepository,
            EmailService emailService,
            EnvUtils envUtils,
            UserRepository userRepository) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantRepository = tenantRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.tenantInvitationRepository = tenantInvitationRepository;
        this.tenantInvitationRoleRepository = tenantInvitationRoleRepository;
        this.emailService = emailService;
        this.envUtils = envUtils;
        this.userRepository = userRepository;
    }

    @Transactional
    public void inviteUserToTenant(InviteUserToTenantRequest dto, UUID tenantId, User user) {
        var email = dto.email();

        /**
         * Checks validity of the request
         */
        var existingTenantUser = tenantUserRepository.findByUserEmailAndTenantId(email, tenantId);
        if (!existingTenantUser.isEmpty()) {
            log.info("Error inviting user_email={} to tenant={}: User is already a member", dto.email(), tenantId);
            throw new IllegalArgumentException("User is already a member");
        }

        /**
         * Revokes active invitations for this email and tenant
         */
        try {
            tenantInvitationRepository.revokeAllActiveByTenantIdAndUserEmail(tenantId, email);
            log.info("Revoked invitations for email={} in tenant={}", email, tenantId);
        } catch (Exception e) {
            log.warn("Error revoking all invitations for tenant={} and email={}: {}",
                    tenantId,
                    email,
                    e.getMessage(),
                    e);
            throw new PersistenceException("Error revoking invitations");
        }

        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DataIntegrityViolationException("Tenant not found"));

        /**
         * Creates invitation with correct assigned roles
         */
        var inviteUserResponse = tenant.inviteUser(email, dto.tenantRoles());
        try {
            tenantRepository.save(tenant);
        } catch (Exception e) {
            log.warn("Error creating tenant invitation for tenant={} and email={}: {}",
                    tenantId,
                    email,
                    e.getMessage(),
                    e);
            throw e;
        }

        emailService.sendSimpleMessage(invitationFrom, email,
                "%s invited you to join them!"
                        .formatted(tenant.getName()),
                "%s invited you to join them! Visit the link below to accept it: \n%s/accept-invitation?code=%s"
                        .formatted(tenant.getName(), frontendUrl, inviteUserResponse.code()));

        if (envUtils.isDevEnvironment()) {
            System.out.println("Tenant invitation code: " + inviteUserResponse.code());
        }

        log.info("Sent invitation to email={}.", email);
    }

    @Transactional
    public void acceptInvitation(AcceptTenantInvitationRequest dto, User user) {
        /**
         * Checks for valid invitations
         */
        var invitations = tenantInvitationRepository
                .findActiveByEmailAndId(user.getEmail(), dto.invitationId());
        if (invitations.isEmpty()) {
            log.info("Active invitation={} not found for email={}", dto.invitationId(), user.getEmail());
            throw new EntityNotFoundException("Invitation not found");
        }
        assert (invitations.size() == 1);

        var invitation = invitations.getFirst();

        /**
         * Creates the tenant user with the correct roles
         */
        var tenantUser = new TenantUser(new TenantUserId(invitation.getTenantId(), user.getId()));
        try {
            tenantUserRepository.save(tenantUser);
        } catch (Exception e) {
            log.warn("Error creating new tenant user for tenant={} and user={}: {}",
                    invitation.getTenantId(),
                    user.getId(),
                    e.getMessage(),
                    e);
            throw e;
        }

        List<TenantUserRole> tenantUserRoles = tenantInvitationRoleRepository
                .findByIdTenantInvitationId(invitation.getId())
                .stream()
                .map(invitationRole -> new TenantUserRole(
                        new TenantUserRoleId(user.getId(), invitation.getTenantId(), invitationRole.getId().roleId())))
                .toList();
        try {
            tenantUserRoleRepository.saveAll(tenantUserRoles);
        } catch (Exception e) {
            log.warn("Error batch saving tenant user roles for tenant={} and user={}: {}",
                    invitation.getTenantId(),
                    user.getId(),
                    e.getMessage(),
                    e);
            throw e;
        }

        /**
         * Accept invitation
         */
        invitation.consume();
        tenantInvitationRepository.save(invitation);

        log.info("User={} accepted invitation to tenant={}", user.getId(), invitation.getTenantId());
    }

    @Transactional(readOnly = true)
    public CheckInvitedUserResponse checkInvitedUser(UUID invitationId) {
        var invitationQueryResult = tenantInvitationRepository.findById(invitationId);
        if (invitationQueryResult.isEmpty() || !invitationQueryResult.get().isValid()) {
            log.info("Invitation={} not found", invitationId);
            throw new EntityNotFoundException("Invitation not found");
        }

        var invitation = invitationQueryResult.get();
        var existingUser = userRepository.findByEmail(invitation.getEmail());

        if (existingUser.isPresent()) {
            return new CheckInvitedUserResponse(CheckInvitedUserNextAction.LOGIN);
        } else {
            return new CheckInvitedUserResponse(CheckInvitedUserNextAction.CREATE_ACCOUNT);
        }
    }

    @Transactional
    public void revokeInvitation(RevokeInvitationRequest dto, UUID tenantId) {
        tenantInvitationRepository.revokeByIdAndTenantId(dto.invitationId(), tenantId);
    }

    @Transactional(readOnly = true)
    public Page<TenantUser> findAllTenantUsers(@Nullable String query, @Nullable Boolean active, Pageable pageable,
            UUID tenantId) {
        var spec = Specification.allOf(
                TenantUserSpecification.hasActiveStatus(active),
                TenantUserSpecification.hasTenantId(tenantId),
                TenantUserSpecification.hasUserName(query));
        return tenantUserRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<TenantUser> findTenantUserById(UUID userId, UUID tenantId) {
        return tenantUserRepository.findById(new TenantUserId(tenantId, userId));
    }

    @Transactional
    public void updateTenantUserActiveStatus(UUID userId, InactivateTenantUserRequest dto, UUID tenantId) {
        var tenantUserId = new TenantUserId(tenantId, userId);
        var tenantUserQueryResult = tenantUserRepository.findById(tenantUserId);
        if (tenantUserQueryResult.isEmpty()) {
            log.info("Tenant user not found for tenant={} and user={}", tenantId, userId);
            throw new EntityNotFoundException("Tenant user not found");
        }

        var tenantUser = tenantUserQueryResult.get();
        if (dto.active()) {
            log.info("Reactivating tenant user for tenant={} and user={}", tenantId, userId);
            tenantUser.reactivate();
        } else {
            log.info("Inactivating tenant user for tenant={} and user={}", tenantId, userId);
            tenantUser.inactivate();
        }
        tenantUserRepository.save(tenantUser);

        log.info("Inactivated tenant user for tenant={} and user={}", tenantId, userId);
    }

    @Transactional
    public void updateTenantUser(UUID userId, UpdateTenantUserRequest dto, UUID tenantId) {
        var tenantUserQueryResult = tenantUserRepository.findById(new TenantUserId(tenantId, userId));
        if (tenantUserQueryResult.isEmpty()) {
            log.info("Tenant user not found for tenant={} and user={}", tenantId, userId);
            throw new EntityNotFoundException("Tenant user not found");
        }

        var tenantUser = tenantUserQueryResult.get();
        Set<TenantUserRole> tenantUserRoles = dto
                .tenantRoleIds()
                .stream()
                .map(tenantRoleId -> new TenantUserRole(new TenantUserRoleId(userId, tenantId, tenantRoleId)))
                .collect(Collectors.toSet());
        tenantUser.setRoles(tenantUserRoles);

        try {
            tenantUserRepository.save(tenantUser);
        } catch (Exception e) {
            log.warn("Error updating tenant user roles for tenant={} and user={}: {}",
                    tenantId,
                    userId,
                    e.getMessage(),
                    e);
            throw e;
        }

        log.debug("Updated tenant user for tenant={} and user={} successfully", tenantId, userId);
    }
}
