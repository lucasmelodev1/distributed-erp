package com.example.demo.tenant.domain.authorization;

import java.time.Instant;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.springframework.data.annotation.CreatedDate;

import com.example.demo.tenant.domain.TenantUser;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_user_roles")
public class TenantUserRole {
	@EmbeddedId
	private TenantUserRoleId id;

	@ManyToOne
	@MapsId("tenantUserId")
	@JoinColumns({
			@JoinColumn(name = "user_id", referencedColumnName = "user_id"),
			@JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
	})
	@JsonIgnore
	@ToStringExclude
	private TenantUser tenantUser;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantUserRole(TenantUserRoleId id) {
		this.id = id;
	}

	public TenantUserRole(TenantUserRoleId id, TenantUser tenantUser) {
		this.id = id;
		this.tenantUser = tenantUser;
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
