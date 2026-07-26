CREATE TABLE tenant_roles (
	id UUID PRIMARY KEY DEFAULT uuidv7(),
	tenant_id UUID NOT NULL REFERENCES tenants(id),

	name TEXT NOT NULL,

	created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ
);

CREATE TABLE tenant_user_roles (
	user_id UUID NOT NULL REFERENCES users(id),
	tenant_id UUID NOT NULL REFERENCES tenants(id),
	tenant_role_id UUID NOT NULL REFERENCES tenant_roles(id),

	created_at TIMESTAMPTZ DEFAULT now(),

	PRIMARY KEY (user_id, tenant_id, tenant_role_id)
);

CREATE TABLE permissions (
	code TEXT PRIMARY KEY,

	description TEXT,

	created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ
);

CREATE TABLE tenant_role_permissions (
	tenant_role_id UUID NOT NULL REFERENCES tenant_roles(id),
	permission_code TEXT NOT NULL REFERENCES permissions(code),

	tenant_id UUID NOT NULL REFERENCES tenants(id),

	created_at TIMESTAMPTZ DEFAULT now(),
	
	PRIMARY KEY (tenant_role_id, permission_code)
);
