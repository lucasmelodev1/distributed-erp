package com.example.demo.tenant.authorization;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_role_permissions")
public class TenantRolePermission {
	@EmbeddedId
	private TenantRolePermissionId id;

	@Column(name = "tenant_id")
	private UUID tenantId;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantRolePermission(TenantRolePermissionId id, UUID tenantId) {
		this.id = id;
		this.tenantId = tenantId;
	}

	public TenantRolePermission() {
	}

	public TenantRolePermissionId getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
