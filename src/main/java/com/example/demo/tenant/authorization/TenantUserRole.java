package com.example.demo.tenant.authorization;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_user_roles")
public class TenantUserRole {
	@EmbeddedId
	private TenantUserRoleId id;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantUserRole(TenantUserRoleId id) {
		this.id = id;
	}

	public TenantUserRole() {
	}

	public TenantUserRoleId getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
