package com.example.demo.tenant.invitation;

import java.time.Instant;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_invitation_roles")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantInvitationRole {
	@EmbeddedId
	private TenantInvitationRoleId id;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantInvitationRole(TenantInvitationRoleId id) {
		this.id = id;
	}

	public TenantInvitationRole() {
	}

	public TenantInvitationRoleId getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
