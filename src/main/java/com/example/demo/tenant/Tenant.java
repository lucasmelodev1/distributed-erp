package com.example.demo.tenant;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import com.example.demo.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Tenant extends BaseEntity {
	public static final int MAX_NAME_SIZE = 255;

	private String name;

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
