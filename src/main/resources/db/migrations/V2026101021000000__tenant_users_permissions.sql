INSERT INTO permissions (code, description) VALUES
('tenant_users:read', 'Read tenant users'),
('tenant_users:update', 'Update tenant user roles');

INSERT INTO tenant_role_permissions (tenant_role_id, permission_code, tenant_id)
SELECT r.id, p.code, r.tenant_id
FROM tenant_roles r
CROSS JOIN permissions p
WHERE r.name = 'Admin'
	AND p.code IN ('tenant_users:read', 'tenant_users:update');
