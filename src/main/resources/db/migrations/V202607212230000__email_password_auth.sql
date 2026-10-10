CREATE TABLE auth_tokens (
	id BINARY(16) NOT NULL PRIMARY KEY,

	type VARCHAR(32) NOT NULL,
	email VARCHAR(255) NOT NULL,
	token_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
	expires_at DATETIME(6) NOT NULL,
	consumed_at DATETIME(6),
	revoked_at DATETIME(6),

	created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_auth_tokens_token_hash ON auth_tokens(token_hash);

CREATE INDEX idx_auth_tokens_active ON auth_tokens(email, type, expires_at);
