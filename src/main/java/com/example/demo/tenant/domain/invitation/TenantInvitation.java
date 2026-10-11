package com.example.demo.tenant.domain.invitation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;

import com.example.demo.tenant.domain.Tenant;
import com.example.demo.utils.HashUtils;
import com.example.demo.utils.UuidV7Id;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * An offer to join a tenant with a set of roles. The invitee proves they received
 * it by presenting the emailed code; only the code's hash is stored.
 */
@Entity
@Table(name = "tenant_invitations")
public class TenantInvitation {
	public enum Status {
		PENDING, ACCEPTED, REVOKED, EXPIRED
	}

	@Id
	@UuidV7Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "consumed_at")
	private @Nullable Instant consumedAt;

	@Column(name = "revoked_at")
	private @Nullable Instant revokedAt;

	@Column(name = "expires_at")
	private @Nullable Instant expiresAt;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	@ManyToOne(optional = false)
	@JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
	private Tenant tenant;

	@OneToMany(mappedBy = "invitation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<TenantInvitationRole> roles = new HashSet<>();

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	protected TenantInvitation() {
	}

	/** The only way to create an invitation: the email is normalised and the code is stored hashed. */
	public static TenantInvitation issue(UUID tenantId, String email, String code, Collection<UUID> roleIds,
			Duration ttl) {
		var invitation = new TenantInvitation();
		invitation.tenantId = tenantId;
		invitation.email = email.trim().toLowerCase(Locale.ROOT);
		invitation.codeHash = HashUtils.hash(code);
		invitation.createdAt = Instant.now();
		invitation.expiresAt = invitation.createdAt.plus(ttl);
		invitation.roles = roleIds.stream().distinct()
				.map(roleId -> new TenantInvitationRole(invitation, roleId))
				.collect(Collectors.toCollection(HashSet::new));
		return invitation;
	}

	/** Whether this invitation was sent to the given address (ignoring case). */
	public boolean isFor(String userEmail) {
		return email.equalsIgnoreCase(userEmail.trim());
	}

	/**
	 * Consumes the invitation. An invitation that is expired, revoked or already
	 * accepted behaves exactly like one that does not exist.
	 */
	public void accept() {
		if (!isValid()) {
			throw new EntityNotFoundException("Invitation not found");
		}
		consume();
	}

	/** Cancels a pending invitation. Revoking twice is harmless; revoking an accepted one is not allowed. */
	public void revoke() {
		if (isConsumed()) {
			throw new IllegalArgumentException("The invitation was already accepted");
		}
		if (!isRevoked()) {
			revokedAt = Instant.now();
		}
	}

	/**
	 * Resending: the plain code is never stored, so the same email cannot be repeated.
	 * The invitation (id, roles) stays, gets a fresh code and a new lifetime, and the
	 * previous link stops working. Accepted and revoked invitations cannot be resent.
	 */
	public void reissue(String newCode, Duration ttl) {
		if (isConsumed()) {
			throw new IllegalArgumentException("The invitation was already accepted");
		}
		if (isRevoked()) {
			throw new IllegalArgumentException("The invitation was revoked. Send a new one instead.");
		}
		codeHash = HashUtils.hash(newCode);
		expiresAt = Instant.now().plus(ttl);
	}

	public void consume() {
		consumedAt = Instant.now();
	}

	public boolean isExpired() {
		return expiresAt != null && expiresAt.isBefore(Instant.now());
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isValid() {
		return !isExpired() && !isConsumed() && !isRevoked();
	}

	public Status getStatus() {
		if (isConsumed()) {
			return Status.ACCEPTED;
		}
		if (isRevoked()) {
			return Status.REVOKED;
		}
		return isExpired() ? Status.EXPIRED : Status.PENDING;
	}

	public Set<UUID> getRoleIds() {
		return roles.stream().map(role -> role.getId().roleId()).collect(Collectors.toSet());
	}

	public UUID getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public String getEmail() {
		return email;
	}

	public @Nullable Instant getConsumedAt() {
		return consumedAt;
	}

	public @Nullable Instant getRevokedAt() {
		return revokedAt;
	}

	public @Nullable Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
