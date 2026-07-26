package com.example.demo.tenant.authorization;

import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import com.example.demo.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_roles")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class TenantRole extends BaseEntity {
	private UUID tenantId;

	private String name;

	public TenantRole(UUID tenantId, String name) {
		this.tenantId = tenantId;
		this.name = name;
	}

	public TenantRole() {}

	public UUID getTenantId() { return tenantId; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
}
