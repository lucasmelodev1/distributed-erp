CREATE TABLE users (
	id UUID PRIMARY KEY DEFAULT uuidv7(),

	name TEXT NOT NULL,
	email TEXT NOT NULL UNIQUE,
	password TEXT,
	profile_icon_url TEXT,
	role TEXT NOT NULL DEFAULT 'USER',

	created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now(),
	deleted_at TIMESTAMPTZ
);
