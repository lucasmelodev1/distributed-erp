package com.example.demo.entity;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class NoIdBaseEntity implements Serializable {

	@CreatedDate
	@Column(name = "created_at")
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private Instant updatedAt;

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
