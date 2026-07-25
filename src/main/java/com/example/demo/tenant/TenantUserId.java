package com.example.demo.tenant;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TenantUserId(
		@Column(name = "tenant_id") UUID tenantId,
		@Column(name = "user_id") UUID userId
) implements Serializable {}
