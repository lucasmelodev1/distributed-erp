package com.example.demo.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.example.demo.tenant.domain.invitation.TenantInvitation;
import com.example.demo.tenant.domain.invitation.TenantInvitation.Status;

import jakarta.persistence.EntityNotFoundException;

class TenantInvitationTest {
	private static final Duration TTL = Duration.ofDays(7);

	private static TenantInvitation pending() {
		return TenantInvitation.issue(UUID.randomUUID(), "  Someone@Mail.com ", "code", List.of(UUID.randomUUID()), TTL);
	}

	/** A fresh invitation that has been taken to the given status through its public transitions. */
	private static TenantInvitation in(Status status) {
		return switch (status) {
			case PENDING -> pending();
			case ACCEPTED -> {
				var invitation = pending();
				invitation.accept();
				yield invitation;
			}
			case REVOKED -> {
				var invitation = pending();
				invitation.revoke();
				yield invitation;
			}
			case EXPIRED -> TenantInvitation.issue(UUID.randomUUID(), "someone@mail.com", "code", List.of(),
					TTL.negated());
		};
	}

	@ParameterizedTest
	@EnumSource(Status.class)
	void status_reflectsTheLifecycle(Status status) {
		var invitation = in(status);

		assertThat(invitation.getStatus()).isEqualTo(status);
		assertThat(invitation.isValid()).isEqualTo(status == Status.PENDING);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
	void accept_behavesLikeAMissingInvitationUnlessPending(Status status) {
		assertThatThrownBy(in(status)::accept).isInstanceOf(EntityNotFoundException.class);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = { "ACCEPTED", "REVOKED" })
	void reissue_isRejectedForAcceptedAndRevoked(Status status) {
		assertThatThrownBy(() -> in(status).reissue("new", TTL)).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = { "PENDING", "EXPIRED" })
	void reissue_givesPendingAndExpiredInvitationsAFreshLifetime(Status status) {
		var invitation = in(status);

		invitation.reissue("new", TTL);

		assertThat(invitation.getStatus()).isEqualTo(Status.PENDING);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
	void revoke_isIdempotentAndWorksUntilAccepted(Status status) {
		var invitation = in(status);

		invitation.revoke();
		invitation.revoke();

		assertThat(invitation.getStatus()).isEqualTo(Status.REVOKED);
	}

	@Test
	void revoke_isRejectedOnceAccepted() {
		assertThatThrownBy(in(Status.ACCEPTED)::revoke).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void issue_normalisesTheEmailAndDeduplicatesRoles() {
		var role = UUID.randomUUID();
		var invitation = TenantInvitation.issue(UUID.randomUUID(), "  Someone@Mail.com ", "code", List.of(role, role),
				TTL);

		assertThat(invitation.getEmail()).isEqualTo("someone@mail.com");
		assertThat(invitation.isFor(" SOMEONE@mail.COM")).isTrue();
		assertThat(invitation.isFor("other@mail.com")).isFalse();
		assertThat(invitation.getRoleIds()).containsExactly(role);
	}
}
