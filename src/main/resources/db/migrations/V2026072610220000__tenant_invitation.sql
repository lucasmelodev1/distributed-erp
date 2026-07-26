CREATE TABLE tenant_invitations (
	id UUID PRIMARY KEY DEFAULT uuidv7(),
	tenant_id UUID NOT NULL REFERENCES tenants(id),	

	email TEXT NOT NULL,
	consumed_at TIMESTAMPTZ,
	revoked_at TIMESTAMPTZ,
	expires_at TIMESTAMPTZ,

	created_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ
);

CREATE TABLE tenant_invitation_roles (
	tenant_invitation_id UUID NOT NULL REFERENCES tenant_invitations(id),
	role_id UUID NOT NULL REFERENCES tenant_roles(id),

	created_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ,

	PRIMARY KEY (tenant_invitation_id, role_id)
);