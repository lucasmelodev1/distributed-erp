INSERT INTO permissions (code, description) VALUES
	('tenants:update', 'Update tenant information'),
	('tenants:delete', 'Delete tenant'),
	('tenants:invite', 'Invite new tenant members'),
	('tenants:expel', 'Expel existing members'),
	('tenant_roles:read', 'Read tenant roles'),
	('tenant_roles:create', 'Create tenant roles'),
	('tenant_roles:update', 'Update tenant roles'),
	('tenant_roles:delete', 'Delete tenant roles'),
	('tenant_user_roles:read', 'Read tenant user roles'),
	('tenant_user_roles:grant', 'Grant tenant user new roles'),
	('tenant_user_roles:revoke', 'Revoke tenant user roles');