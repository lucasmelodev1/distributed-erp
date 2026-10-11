INSERT INTO permissions (code, description, level) VALUES
('products:read', 'Read products', 'VIEW'),
('products:write', 'Create, update, inactivate and reactivate products', 'EDIT'),
('products:delete', 'Delete products', 'SENSITIVE');

INSERT INTO tenant_role_permissions (tenant_role_id, permission_code, tenant_id)
SELECT r.id, p.code, r.tenant_id
FROM tenant_roles r
CROSS JOIN permissions p
WHERE r.name = 'Admin'
	AND r.built_in = TRUE
	AND p.code IN ('products:read', 'products:write', 'products:delete');
