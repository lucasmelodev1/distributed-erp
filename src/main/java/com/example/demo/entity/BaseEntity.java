package com.example.demo.entity;

import java.io.Serializable;
import java.util.UUID;

import com.example.demo.utils.UuidV7Id;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity extends NoIdBaseEntity {

	@Id
	@UuidV7Id
	private UUID id;

	public UUID getId() {
		return id;
	}
}
