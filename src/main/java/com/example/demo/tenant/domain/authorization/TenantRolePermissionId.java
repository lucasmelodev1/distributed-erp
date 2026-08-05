package com.example.demo.tenant.domain.authorization;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TenantRolePermissionId(
		@Column(name = "tenant_role_id") UUID tenantRoleId,
		@Column(name = "permission_code") String permissionCode) implements Serializable {
}
