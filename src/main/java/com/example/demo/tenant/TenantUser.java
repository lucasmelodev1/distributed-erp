package com.example.demo.tenant;

import java.time.Instant;
import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.example.demo.entity.NoIdBaseEntity;
import com.example.demo.tenant.authorization.tenantuserrole.TenantUserRole;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_users")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantUser extends NoIdBaseEntity {
	@EmbeddedId
	private TenantUserId id;

	private Instant inactivatedAt;

	@OneToMany(mappedBy = "tenantUser", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.LAZY)
	private Set<TenantUserRole> roles;

	public Set<TenantUserRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<TenantUserRole> roles) {
		this.roles = roles;
	}

	public Instant getInactivatedAt() {
		return inactivatedAt;
	}

	public boolean isActive() {
		return inactivatedAt == null;
	}

	public void inactivate() {
		this.inactivatedAt = Instant.now();
	}

	public void reactivate() {
		this.inactivatedAt = null;
	}

	public TenantUser(TenantUserId id) {
		this.id = id;
	}

	public TenantUser(TenantUserId id, Set<TenantUserRole> roles) {
		this.id = id;
		this.roles = roles;
	}

	public TenantUser() {
	}

	public TenantUserId getId() {
		return id;
	}
}
