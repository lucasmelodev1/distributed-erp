CREATE TABLE products (
	id BINARY(16) NOT NULL PRIMARY KEY,
	tenant_id BINARY(16) NOT NULL,

	name VARCHAR(255) NOT NULL,
	sku VARCHAR(100),
	description VARCHAR(1000),
	price DECIMAL(19, 10) NOT NULL,

	inactivated_at DATETIME(6),

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
	deleted_at DATETIME(6),

	CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
	-- Soft-deleted products keep their SKU. Several products may have no SKU (NULLs are distinct).
	CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku),
	INDEX idx_products_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
