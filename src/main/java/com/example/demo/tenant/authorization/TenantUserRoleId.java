package com.example.demo.tenant.authorization;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TenantUserRoleId(
		@Column(name = "user_id") UUID userId,
		@Column(name = "tenant_id") UUID tenantId,
		@Column(name = "tenant_role_id") UUID tenantRoleId) implements Serializable {
}
