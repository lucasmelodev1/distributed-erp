ALTER TABLE tenant_invitations ADD COLUMN code_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin;

UPDATE tenant_invitations SET code_hash = '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08';

ALTER TABLE tenant_invitations MODIFY COLUMN code_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;

ALTER TABLE tenant_invitations ADD CONSTRAINT code_hash_unique UNIQUE (code_hash);

ALTER TABLE tenant_invitations DROP COLUMN deleted_at;
