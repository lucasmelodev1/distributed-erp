package com.example.demo.tenant.domain.authorization;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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

	/** The default role of a tenant (Admin): it can never be changed through the API. */
	private boolean builtIn;

	@OneToMany(mappedBy = "role", orphanRemoval = true, cascade = jakarta.persistence.CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
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

	/** The default Admin role of a tenant. */
	public static TenantRole builtIn(UUID tenantId, String name, Set<TenantRolePermission> permissions) {
		var role = new TenantRole(tenantId, name, permissions);
		role.builtIn = true;
		return role;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	/** Fails for the built-in role; every change to a role's name, permissions or status goes through this rule. */
	public void requireChangeable() {
		if (builtIn) {
			throw new IllegalArgumentException("The default role cannot be changed");
		}
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
		requireChangeable();
		if (isActive()) {
			this.inactivatedAt = Instant.now();
		}
	}

	public void reactivate() {
		requireChangeable();
		this.inactivatedAt = null;
	}

	public void setName(String name) {
		requireChangeable();
		this.name = name;
	}
}
