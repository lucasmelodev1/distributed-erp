package com.example.demo.tenant.domain.authorization;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import com.example.demo.entity.NoIdBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Permission extends NoIdBaseEntity {
	@Id
	private String code;

	private String description;

	public Permission(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public Permission() {
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
