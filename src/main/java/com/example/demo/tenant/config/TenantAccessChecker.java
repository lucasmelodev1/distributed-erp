package com.example.demo.tenant.config;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;


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

	private final AccessSnapshotStore snapshotStore;

	public TenantAccessChecker(AccessSnapshotStore snapshotStore) {
		this.snapshotStore = snapshotStore;
	}

	/** The user's cached access data; loaded from the database only when the session has none or it is stale. */
	public AccessSnapshot snapshot(HttpServletRequest request, UUID userId) {
		return snapshotStore.snapshotFor(request, userId);
	}

	public Decision check(AccessSnapshot snapshot, UUID tenantId, TenantAware requirements) {
		var access = snapshot.tenants().get(tenantId);
		if (access == null) {
			return new Decision(Denial.NOT_A_MEMBER, null);
		}
		if (!access.memberActive()) {
			return new Decision(Denial.MEMBERSHIP_INACTIVE, null);
		}
		if (!access.tenantActive() && !requirements.allowInactiveTenant()) {
			return new Decision(Denial.TENANT_INACTIVE, null);
		}
		for (String permission : requirements.withPermissions()) {
			if (!access.permissions().contains(permission)) {
				return new Decision(Denial.MISSING_PERMISSION, permission);
			}
		}
		return Decision.GRANTED;
	}
}
