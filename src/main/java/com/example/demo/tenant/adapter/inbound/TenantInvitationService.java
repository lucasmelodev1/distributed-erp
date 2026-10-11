package com.example.demo.tenant.adapter.inbound;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.email.EmailService;
import com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRoleRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantInvitationRoleRepository.InvitationRoleRow;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.domain.Tenant;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.domain.invitation.TenantInvitation;
import com.example.demo.tenant.domain.invitation.TenantInvitationSpecification;
import com.example.demo.tenant.port.inbound.TenantInvitationPort;
import com.example.demo.user.adapter.web.dto.user.CreateUserInput;
import com.example.demo.user.domain.User;
import com.example.demo.user.port.inbound.UserProfilePort;
import com.example.demo.utils.EnvUtils;
import com.example.demo.utils.HashUtils;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantInvitationService implements TenantInvitationPort {
	private static final Logger log = LoggerFactory.getLogger(TenantInvitationService.class);

	private final TenantInvitationRepository invitationRepository;
	private final TenantInvitationRoleRepository invitationRoleRepository;
	private final TenantRepository tenantRepository;
	private final TenantUserRepository tenantUserRepository;
	private final TenantRoleChecks roleChecks;
	private final UserProfilePort userService;
	private final EmailService emailService;
	private final EnvUtils envUtils;

	@Value("${TENANT_INVITATION_FROM:invitations@example.com}")
	private String invitationFrom;

	@Value("${FRONTEND_URL:http://localhost:5173}")
	private String frontendUrl;

	@Value("${app.invitations.ttl:3d}")
	private Duration ttl;

	TenantInvitationService(
			TenantInvitationRepository invitationRepository,
			TenantInvitationRoleRepository invitationRoleRepository,
			TenantRepository tenantRepository,
			TenantUserRepository tenantUserRepository,
			TenantRoleChecks roleChecks,
			UserProfilePort userService,
			EmailService emailService,
			EnvUtils envUtils) {
		this.invitationRepository = invitationRepository;
		this.invitationRoleRepository = invitationRoleRepository;
		this.tenantRepository = tenantRepository;
		this.tenantUserRepository = tenantUserRepository;
		this.roleChecks = roleChecks;
		this.userService = userService;
		this.emailService = emailService;
		this.envUtils = envUtils;
	}

	@Transactional
	public TenantInvitation inviteUserToTenant(String email, Collection<UUID> tenantRoleIds, UUID inviterId,
			UUID tenantId) {
		var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

		if (!tenantUserRepository.findByUserEmailAndTenantId(normalizedEmail, tenantId).isEmpty()) {
			log.info("Error inviting email={} to tenant={}: already a member", normalizedEmail, tenantId);
			throw new IllegalArgumentException(
					"User is already a member. If their membership is inactive, reactivate it instead.");
		}
		roleChecks.requireActiveRolesOfTenant(tenantRoleIds, tenantId);
		roleChecks.requireGrantable(tenantRoleIds, inviterId, tenantId);
		var tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

		invitationRepository.revokeAllActiveByTenantIdAndUserEmail(tenantId, normalizedEmail);

		var code = UUID.randomUUID().toString();
		var invitation = invitationRepository.save(tenant.inviteUser(normalizedEmail, tenantRoleIds, code, ttl));
		sendInvitation(tenant.getName(), normalizedEmail, code);

		log.info("Invited email={} to tenant={}", normalizedEmail, tenantId);
		return invitation;
	}

	@Transactional
	public TenantInvitation resendInvitation(UUID invitationId, UUID tenantId) {
		var invitation = invitationRepository.findByIdAndTenantId(invitationId, tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Invitation not found"));
		var tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

		var code = UUID.randomUUID().toString();
		invitation.reissue(code, ttl);
		invitationRepository.save(invitation);
		sendInvitation(tenant.getName(), invitation.getEmail(), code);

		log.info("Resent invitation={} to tenant={}", invitationId, tenantId);
		return invitation;
	}

	@Transactional
	public TenantInvitation revokeInvitation(UUID invitationId, UUID tenantId) {
		var invitation = invitationRepository.findByIdAndTenantId(invitationId, tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Invitation not found"));
		invitation.revoke();
		return invitationRepository.save(invitation);
	}

	@Transactional(readOnly = true)
	public Window<TenantInvitation> scrollInvitations(TenantInvitation.@Nullable Status status, @Nullable String query,
			int limit,
			@Nullable UUID after, UUID tenantId) {
		var spec = Specification.allOf(
				TenantInvitationSpecification.hasTenantId(tenantId),
				TenantInvitationSpecification.hasStatus(status, Instant.now()),
				TenantInvitationSpecification.hasEmailContaining(query));
		ScrollPosition position = after == null ? ScrollPosition.keyset()
				: ScrollPosition.forward(Map.of("id", after));
		return invitationRepository.findBy(spec, q -> q.sortBy(Sort.by("id")).limit(limit).scroll(position));
	}

	@Transactional(readOnly = true)
	public Map<UUID, List<TenantRole>> findRolesByInvitationIds(Collection<UUID> invitationIds, UUID tenantId) {
		if (invitationIds.isEmpty()) {
			return Map.of();
		}
		return invitationRoleRepository.findRolesByInvitationIds(invitationIds, tenantId).stream()
				.collect(Collectors.groupingBy(InvitationRoleRow::invitationId,
						Collectors.mapping(InvitationRoleRow::role, Collectors.toList())));
	}

	@Transactional(readOnly = true)
	public InvitationCheck checkInvitedUser(String code, @Nullable User currentUser) {
		var invitation = findValidByCode(code);
		var tenant = requireTenant(invitation);

		InvitationCheck.NextAction nextAction;
		if (currentUser != null) {
			nextAction = invitation.isFor(currentUser.getEmail()) ? InvitationCheck.NextAction.ACCEPT
					: InvitationCheck.NextAction.WRONG_ACCOUNT;
		} else {
			nextAction = userService.existsByEmail(invitation.getEmail()) ? InvitationCheck.NextAction.LOGIN
					: InvitationCheck.NextAction.CREATE_ACCOUNT;
		}
		return new InvitationCheck(nextAction, invitation.getEmail(), tenant.getName());
	}

	@Transactional
	public Tenant acceptInvitation(String code, User user) {
		var invitation = findValidByCode(code);
		if (!invitation.isFor(user.getEmail())) {
			log.info("User={} tried to accept invitation={} sent to another email", user.getId(), invitation.getId());
			throw new AccessDeniedException("This invitation was sent to a different email address");
		}
		var tenant = requireTenant(invitation);

		joinTenant(invitation, user.getId());
		invitation.accept();
		invitationRepository.save(invitation);

		log.info("User={} accepted invitation={} to tenant={}", user.getId(), invitation.getId(), tenant.getId());
		return tenant;
	}

	@Transactional
	public RegisteredUser registerFromInvitation(String code, String name, String password) {
		var invitation = findValidByCode(code);
		if (userService.existsByEmail(invitation.getEmail())) {
			throw new IllegalArgumentException(
					"An account with this email already exists. Log in to accept the invitation.");
		}

		var created = userService.createUser(new CreateUserInput(name, invitation.getEmail(), password));
		joinTenant(invitation, created.id());
		invitation.accept();
		invitationRepository.save(invitation);

		log.info("User={} registered from invitation={} to tenant={}", created.id(), invitation.getId(),
				invitation.getTenantId());
		return new RegisteredUser(created.id(), invitation.getEmail());
	}

	private TenantInvitation findValidByCode(String code) {
		return invitationRepository.findByCodeHash(HashUtils.hash(code.trim()))
				.filter(TenantInvitation::isValid)
				.orElseThrow(() -> new EntityNotFoundException("Invitation not found"));
	}

	private Tenant requireTenant(TenantInvitation invitation) {
		return tenantRepository.findById(invitation.getTenantId())
				.orElseThrow(() -> new EntityNotFoundException("Invitation not found"));
	}

	/**
	 * Adds the user to the tenant with the invited roles that still exist. A member
	 * who was removed earlier gets their old membership restored.
	 */
	private void joinTenant(TenantInvitation invitation, UUID userId) {
		var tenantId = invitation.getTenantId();
		var roleIds = roleChecks.rolesOfTenant(invitation.getRoleIds(), tenantId);
		var id = new TenantUserId(tenantId, userId);

		var existing = tenantUserRepository.findById(id);
		if (existing.isPresent()) {
			throw new IllegalArgumentException(existing.get().isActive()
					? "You are already a member of this tenant"
					: "Your membership in this tenant is inactive. Ask an administrator to reactivate it.");
		}

		if (tenantUserRepository.restoreRemoved(tenantId, userId) == 1) {
			var restored = tenantUserRepository.findById(id).orElseThrow();
			restored.replaceRoles(roleIds);
			tenantUserRepository.save(restored);
		} else {
			tenantUserRepository.save(TenantUser.joining(tenantId, userId, roleIds));
		}
	}

	/** The email service publishes only after the transaction commits, so a rolled-back invitation sends nothing. */
	private void sendInvitation(String tenantName, String email, String code) {
		var name = tenantName.replaceAll("[\\r\\n]+", " ");
		var link = "%s/accept-invitation?code=%s".formatted(frontendUrl, code);
		try {
			emailService.sendSimpleMessage(invitationFrom, email,
					"%s invited you to join them!".formatted(name),
					"%s invited you to join them! Visit the link below to accept it: \n%s".formatted(name, link));
		} catch (Exception e) {
			log.warn("Error sending invitation email to email={}: {}", email, e.getMessage(), e);
		}
		if (envUtils.isDevEnvironment()) {
			System.out.println("Tenant invitation link: " + link);
		}
	}
}
