package com.example.demo.user.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;

import com.example.demo.utils.UuidV7Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {
	public static final int TOKEN_SIZE = 6;

	@Id
	@UuidV7Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, updatable = false)
	private AuthTokenType type;

	@Column(name = "email", nullable = false, updatable = false)
	private String email;

	@Column(name = "token_hash", nullable = false, updatable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));

	@Column(name = "consumed_at")
	private @Nullable Instant consumedAt;

	@Column(name = "revoked_at")
	private @Nullable Instant revokedAt;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public AuthToken() {
	}

	public AuthToken(String email, String tokenHash, AuthTokenType type) {
		this.email = email;
		this.tokenHash = tokenHash;
		this.type = type;
	}

	public void revoke() {
		revokedAt = Instant.now();
	}

	public void consume() {
		consumedAt = Instant.now();
	}

	public boolean isExpired() {
		return expiresAt.isBefore(Instant.now());
	}

	public boolean isVerified() {
		return consumedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isValid() {
		return !isExpired() && !isVerified() && !isRevoked();
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) throws IllegalArgumentException {
		if (!new EmailValidator().isValid(email, null)) {
			throw new IllegalArgumentException("Invalid email: " + email);
		}
		this.email = email;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public AuthTokenType getType() {
		return type;
	}

	public void setType(AuthTokenType type) {
		this.type = type;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public void setConsumedAt(Instant verifiedAt) {
		this.consumedAt = verifiedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
