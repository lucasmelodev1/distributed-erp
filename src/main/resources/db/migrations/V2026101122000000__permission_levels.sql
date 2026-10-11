ALTER TABLE permissions ADD COLUMN level VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin;

UPDATE permissions SET level = CASE code
	WHEN 'tenants:update' THEN 'EDIT'
	WHEN 'tenants:invite' THEN 'EDIT'
	WHEN 'tenants:delete' THEN 'SENSITIVE'
	WHEN 'tenants:expel' THEN 'SENSITIVE'
	WHEN 'tenants:inactivate' THEN 'SENSITIVE'
	WHEN 'tenant_roles:read' THEN 'VIEW'
	WHEN 'tenant_roles:create' THEN 'EDIT'
	WHEN 'tenant_roles:update' THEN 'EDIT'
	WHEN 'tenant_roles:delete' THEN 'SENSITIVE'
	WHEN 'tenant_roles:inactivate' THEN 'SENSITIVE'
	WHEN 'tenant_user_roles:read' THEN 'VIEW'
	WHEN 'tenant_user_roles:grant' THEN 'EDIT'
	WHEN 'tenant_user_roles:revoke' THEN 'EDIT'
	WHEN 'tenant_users:read' THEN 'VIEW'
	WHEN 'tenant_users:update' THEN 'EDIT'
	WHEN 'tenant_users:inactivate' THEN 'SENSITIVE'
END;

ALTER TABLE permissions MODIFY COLUMN level VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;
