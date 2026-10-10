ALTER TABLE tenant_users ADD COLUMN inactivated_at DATETIME(6);
ALTER TABLE tenant_roles ADD COLUMN inactivated_at DATETIME(6);
ALTER TABLE tenants ADD COLUMN inactivated_at DATETIME(6);

INSERT INTO permissions (code, description) VALUES
('tenant_users:inactivate', 'Inactivate tenant users'),
('tenant_roles:inactivate', 'Inactivate tenant roles'),
('tenants:inactivate', 'Inactivate tenants');
