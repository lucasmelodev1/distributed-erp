CREATE TABLE tenant_roles (
	id BINARY(16) NOT NULL PRIMARY KEY,
	tenant_id BINARY(16) NOT NULL,

	name VARCHAR(255) NOT NULL,

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	deleted_at DATETIME(6),

	CONSTRAINT fk_tenant_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tenant_user_roles (
	user_id BINARY(16) NOT NULL,
	tenant_id BINARY(16) NOT NULL,
	tenant_role_id BINARY(16) NOT NULL,

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (user_id, tenant_id, tenant_role_id),
	CONSTRAINT fk_tenant_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
	CONSTRAINT fk_tenant_user_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
	CONSTRAINT fk_tenant_user_roles_role FOREIGN KEY (tenant_role_id) REFERENCES tenant_roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permissions (
	code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,

	description VARCHAR(255),

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	deleted_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tenant_role_permissions (
	tenant_role_id BINARY(16) NOT NULL,
	permission_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

	tenant_id BINARY(16) NOT NULL,

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (tenant_role_id, permission_code),
	CONSTRAINT fk_trp_role FOREIGN KEY (tenant_role_id) REFERENCES tenant_roles(id),
	CONSTRAINT fk_trp_permission FOREIGN KEY (permission_code) REFERENCES permissions(code),
	CONSTRAINT fk_trp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
