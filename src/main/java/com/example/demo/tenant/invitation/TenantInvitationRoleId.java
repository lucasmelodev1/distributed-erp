package com.example.demo.tenant.invitation;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TenantInvitationRoleId(
		@Column(name = "tenant_invitation_id") UUID tenantInvitationId,
		@Column(name = "role_id") UUID roleId) implements Serializable {
}
