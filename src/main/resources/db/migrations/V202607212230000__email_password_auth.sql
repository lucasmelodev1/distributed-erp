CREATE TABLE auth_tokens (
	id UUID PRIMARY KEY DEFAULT uuidv7(),

	type TEXT NOT NULL,
	email TEXT NOT NULL,
	token_hash TEXT NOT NULL,
	expires_at TIMESTAMPTZ NOT NULL,
	consumed_at TIMESTAMPTZ,
	revoked_at TIMESTAMPTZ,

	created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_auth_tokens_token_hash ON auth_tokens(token_hash);

CREATE INDEX idx_auth_tokens_active ON auth_tokens(lower(email), type, expires_at)
WHERE consumed_at IS NULL AND revoked_at IS NULL;
