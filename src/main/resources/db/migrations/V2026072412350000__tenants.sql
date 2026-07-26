CREATE TABLE tenants (
	id UUID PRIMARY KEY DEFAULT uuidv7(),

	name TEXT NOT NULL,

	created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ
);

CREATE TABLE tenant_users (
	user_id UUID REFERENCES users(id),
	tenant_id UUID REFERENCES tenants(id),

	created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ,

	PRIMARY KEY (user_id, tenant_id)
);
