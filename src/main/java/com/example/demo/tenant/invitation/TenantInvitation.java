package com.example.demo.tenant.invitation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;

import com.example.demo.utils.UuidV7Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_invitations")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
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

	public TenantInvitation() {
	}

	public TenantInvitation(UUID tenantId, String email) {
		this.tenantId = tenantId;
		this.email = email;
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
