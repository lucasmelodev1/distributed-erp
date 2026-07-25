package com.example.demo.tenant;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.example.demo.entity.NoIdBaseEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_users")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantUser extends NoIdBaseEntity {
	@EmbeddedId
	private TenantUserId id;

	public TenantUser(TenantUserId id) {
		this.id = id;
	}

	public TenantUser() {
	}

	public TenantUserId getId() {
		return id;
	}
}
