package com.example.demo.tenant.domain.authorization;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.springframework.data.annotation.CreatedDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_role_permissions")
public class TenantRolePermission {
	@EmbeddedId
	private TenantRolePermissionId id;

	@ManyToOne
	@MapsId("tenantRoleId")
	@JoinColumn(name = "tenant_role_id")
	@JsonIgnore
	@ToStringExclude
	private TenantRole role;

	@Column(name = "tenant_id")
	private UUID tenantId;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantRolePermission(TenantRole role, String permissionCode, UUID tenantId) {
		this.role = role;
		this.id = new TenantRolePermissionId(null, permissionCode);
		this.tenantId = tenantId;
	}

	public TenantRolePermission(TenantRolePermissionId id, UUID tenantId) {
		this.id = id;
		this.tenantId = tenantId;
	}

	public TenantRolePermission() {
	}

	public TenantRolePermissionId getId() {
		return id;
	}

	public TenantRole getRole() {
		return role;
	}

	public void setRole(TenantRole role) {
		this.role = role;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
