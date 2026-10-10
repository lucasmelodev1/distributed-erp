package com.example.demo.tenant.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.example.demo.entity.NoIdBaseEntity;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

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

	/**
	 * Makes the member hold exactly these roles, keeping the existing collection
	 * instance so orphan removal can delete the dropped ones.
	 */
	public void replaceRoles(Collection<UUID> tenantRoleIds) {
		if (roles == null) {
			roles = new HashSet<>();
		}
		var wanted = new HashSet<>(tenantRoleIds);
		roles.removeIf(role -> !wanted.contains(role.getId().tenantRoleId()));
		roles.stream().map(role -> role.getId().tenantRoleId()).forEach(wanted::remove);
		wanted.forEach(roleId -> roles.add(new TenantUserRole(
				new TenantUserRoleId(id.userId(), id.tenantId(), roleId), this)));
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

	/** A new membership holding exactly the given roles. */
	public static TenantUser joining(UUID tenantId, UUID userId, Collection<UUID> tenantRoleIds) {
		var tenantUser = new TenantUser(new TenantUserId(tenantId, userId));
		tenantUser.replaceRoles(tenantRoleIds);
		return tenantUser;
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
