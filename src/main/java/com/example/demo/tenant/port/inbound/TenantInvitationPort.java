package com.example.demo.tenant.port.inbound;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Window;

import com.example.demo.tenant.domain.Tenant;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.domain.invitation.TenantInvitation;
import com.example.demo.user.domain.User;

public interface TenantInvitationPort {

	/** What the person holding an invitation code has to do next. */
	record InvitationCheck(NextAction nextAction, String email, String tenantName) {
		public enum NextAction {
			/** An account exists for the invited email; log in, then accept. */
			LOGIN,
			/** No account exists; register from the invitation. */
			CREATE_ACCOUNT,
			/** Logged in as the invited email; accept. */
			ACCEPT,
			/** Logged in as somebody else. */
			WRONG_ACCOUNT
		}
	}

	record RegisteredUser(UUID userId, String email) {
	}

	/** Issues an invitation and emails the code once the transaction commits. */
	TenantInvitation inviteUserToTenant(String email, Collection<UUID> tenantRoleIds, UUID tenantId);

	TenantInvitation revokeInvitation(UUID invitationId, UUID tenantId);

	/** Forward keyset page ordered by id (creation time). */
	Window<TenantInvitation> scrollInvitations(TenantInvitation.@Nullable Status status, int limit,
			@Nullable UUID after, UUID tenantId);

	Map<UUID, List<TenantRole>> findRolesByInvitationIds(Collection<UUID> invitationIds, UUID tenantId);

	/** Side-effect free. Unknown, expired, revoked and used codes are all "not found". */
	InvitationCheck checkInvitedUser(String code, @Nullable User currentUser);

	Tenant acceptInvitation(String code, User user);

	/** Creates the account for the invited email and accepts the invitation; the caller then logs the user in. */
	RegisteredUser registerFromInvitation(String code, String name, String password);
}
