package com.example.demo.tenant.domain;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.example.demo.entity.BaseEntity;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantResponse;
import com.example.demo.tenant.domain.invitation.TenantInvitation;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Tenant extends BaseEntity {
	public static final int MAX_NAME_SIZE = 255;

	private String name;

	@OneToMany(mappedBy = "tenant")
	private Set<TenantInvitation> invitations;

	public InviteUserToTenantResponse inviteUser(String email, Set<UUID> tenantRoleIds) {
		String code = UUID.randomUUID().toString();
		TenantInvitation invitation = new TenantInvitation(this.getId(), email, code, tenantRoleIds);
		invitations.add(invitation);
		return new InviteUserToTenantResponse(code);
	}

	public Tenant(String name) {
		this.name = name;
	}

	public Tenant() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
