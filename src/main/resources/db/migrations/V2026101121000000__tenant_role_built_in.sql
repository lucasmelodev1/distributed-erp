ALTER TABLE tenant_roles ADD COLUMN built_in BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tenant_roles SET built_in = TRUE WHERE name = 'Admin';
