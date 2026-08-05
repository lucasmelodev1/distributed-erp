package com.example.demo.tenant.domain.invitation;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;

import com.example.demo.tenant.domain.Tenant;
import com.example.demo.utils.HashUtils;
import com.example.demo.utils.UuidV7Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_invitations")
public class TenantInvitation {
	private static final TemporalAmount EXPIRATION_TIME = Duration.ofDays(3);

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
	private @Nullable Instant expiresAt = Instant.now().plus(EXPIRATION_TIME);

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	@ManyToOne(optional = false)
	@JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
	private Tenant tenant;

	@OneToMany(mappedBy = "invitation", fetch = FetchType.LAZY)
	private Set<TenantInvitationRole> roles = new HashSet<>();

	@Column(name = "code_hash", nullable = false, updatable = false)
	private String codeHash;

	public TenantInvitation() {
	}

	public TenantInvitation(UUID tenantId, String email, String code) throws NoSuchAlgorithmException {
		this.tenantId = tenantId;
		this.email = email;
		this.codeHash = HashUtils.hash(code);
	}

	public TenantInvitation(UUID tenantId, String email, String code, Set<UUID> roleIds) {
		this.tenantId = tenantId;
		this.email = email;
		this.codeHash = HashUtils.hash(code);
		this.roles = roleIds.stream()
				.map(roleId -> new TenantInvitationRole(new TenantInvitationRoleId(this.id, roleId)))
				.collect(java.util.stream.Collectors.toSet());
	}

	public boolean checkCode(String code) throws NoSuchAlgorithmException {
		return HashUtils.verifyHash(code, this.codeHash);
	}

	public void revoke() {
		revokedAt = Instant.now();
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
