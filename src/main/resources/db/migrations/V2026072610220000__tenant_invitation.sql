CREATE TABLE tenant_invitations (
	id BINARY(16) NOT NULL PRIMARY KEY,
	tenant_id BINARY(16) NOT NULL,

	email VARCHAR(255) NOT NULL,
	consumed_at DATETIME(6),
	revoked_at DATETIME(6),
	expires_at DATETIME(6),

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	deleted_at DATETIME(6),

	CONSTRAINT fk_tenant_invitations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tenant_invitation_roles (
	tenant_invitation_id BINARY(16) NOT NULL,
	role_id BINARY(16) NOT NULL,

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	deleted_at DATETIME(6),

	PRIMARY KEY (tenant_invitation_id, role_id),
	CONSTRAINT fk_tir_invitation FOREIGN KEY (tenant_invitation_id) REFERENCES tenant_invitations(id),
	CONSTRAINT fk_tir_role FOREIGN KEY (role_id) REFERENCES tenant_roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
