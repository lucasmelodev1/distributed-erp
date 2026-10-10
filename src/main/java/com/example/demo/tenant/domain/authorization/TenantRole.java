package com.example.demo.tenant.domain.authorization;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import com.example.demo.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_roles")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantRole extends BaseEntity {
	public static final int MAX_NAME_LENGTH = 255;

	private UUID tenantId;

	private String name;

	private Instant inactivatedAt;

	@OneToMany(mappedBy = "role", orphanRemoval = true, cascade = jakarta.persistence.CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	@ToStringExclude
	private Set<TenantRolePermission> permissions;

	public Set<TenantRolePermission> getPermissions() {
		return permissions;
	}

	public TenantRole(UUID tenantId, String name, Set<TenantRolePermission> permissions) {
		this.tenantId = tenantId;
		this.name = name;
		this.permissions = permissions;
	}

	public TenantRole() {
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public String getName() {
		return name;
	}

	public Instant getInactivatedAt() {
		return inactivatedAt;
	}

	public boolean isActive() {
		return inactivatedAt == null;
	}

	/** An inactive role stays assigned to its holders but grants no permissions. */
	public void inactivate() {
		if (isActive()) {
			this.inactivatedAt = Instant.now();
		}
	}

	public void reactivate() {
		this.inactivatedAt = null;
	}

	public void setName(String name) {
		this.name = name;
	}
}
