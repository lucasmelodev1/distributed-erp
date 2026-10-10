package com.example.demo.tenant.config;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.example.demo.tenant.adapter.outbound.persistence.PermissionCheckRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository.MembershipState;

/**
 * The single place that decides whether a user may run a {@link TenantAware}
 * operation in a tenant. Called by the GraphQL access instrumentation.
 *
 * <p>Rules, in order: the user must be a member; the membership must be active;
 * the tenant must be active (unless the operation allows inactive tenants); and
 * every required permission must come from an active role.
 */
@Component
public class TenantAccessChecker {

	public enum Denial {
		NOT_A_MEMBER("You are not a member of this tenant"),
		MEMBERSHIP_INACTIVE("Your membership in this tenant is inactive"),
		TENANT_INACTIVE("This tenant is inactive"),
		MISSING_PERMISSION("Missing required permission");

		private final String message;

		Denial(String message) {
			this.message = message;
		}

		public String message() {
			return message;
		}
	}

	/** Outcome of a check; {@code detail} names the missing permission, if that was the reason. */
	public record Decision(@Nullable Denial denial, @Nullable String detail) {
		static final Decision GRANTED = new Decision(null, null);

		public boolean granted() {
			return denial == null;
		}

		/** Human-readable reason, safe to return to the caller. */
		public String message() {
			if (denial == null) {
				return "";
			}
			return detail == null ? denial.message() : denial.message() + ": " + detail;
		}
	}

	private final TenantUserRepository tenantUserRepository;
	private final PermissionCheckRepository permissionCheckRepository;

	public TenantAccessChecker(TenantUserRepository tenantUserRepository,
			PermissionCheckRepository permissionCheckRepository) {
		this.tenantUserRepository = tenantUserRepository;
		this.permissionCheckRepository = permissionCheckRepository;
	}

	/** Loads the membership and tenant state; callers may cache it for the duration of a request. */
	public Optional<MembershipState> findMembership(UUID tenantId, UUID userId) {
		return tenantUserRepository.findMembershipState(tenantId, userId);
	}

	public Decision check(Optional<MembershipState> membership, UUID tenantId, UUID userId,
			TenantAware requirements) {
		if (membership.isEmpty()) {
			return new Decision(Denial.NOT_A_MEMBER, null);
		}
		if (!membership.get().memberActive()) {
			return new Decision(Denial.MEMBERSHIP_INACTIVE, null);
		}
		if (!membership.get().tenantActive() && !requirements.allowInactiveTenant()) {
			return new Decision(Denial.TENANT_INACTIVE, null);
		}
		for (String permission : requirements.withPermissions()) {
			if (!permissionCheckRepository.hasPermission(userId, tenantId, permission)) {
				return new Decision(Denial.MISSING_PERMISSION, permission);
			}
		}
		return Decision.GRANTED;
	}
}
