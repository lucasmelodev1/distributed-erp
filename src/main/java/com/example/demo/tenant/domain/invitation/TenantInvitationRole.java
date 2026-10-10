package com.example.demo.tenant.domain.invitation;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringExclude;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_invitation_roles")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantInvitationRole {
	@EmbeddedId
	private TenantInvitationRoleId id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@MapsId("tenantInvitationId")
	@JoinColumn(name = "tenant_invitation_id", referencedColumnName = "id", insertable = false, updatable = false)
	@JsonIgnore
	@ToStringExclude
	private TenantInvitation invitation;

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	public TenantInvitationRole(TenantInvitationRoleId id) {
		this.id = id;
	}

	/** The invitation id is derived from the association when the invitation is persisted. */
	TenantInvitationRole(TenantInvitation invitation, UUID roleId) {
		this.invitation = invitation;
		this.id = new TenantInvitationRoleId(null, roleId);
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
