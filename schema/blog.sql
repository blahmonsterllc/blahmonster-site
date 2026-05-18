-- Run after creating D1 database "blahmonster-blog":
--   npx wrangler d1 execute blahmonster-blog --remote --file=schema/blog.sql
-- Or for local:
--   npx wrangler d1 execute blahmonster-blog --local --file=schema/blog.sql

CREATE TABLE IF NOT EXISTS posts (
	id TEXT PRIMARY KEY,
	title TEXT NOT NULL,
	slug TEXT NOT NULL UNIQUE,
	date TEXT NOT NULL,
	tag TEXT,
	excerpt TEXT NOT NULL,
	content TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_posts_date ON posts (date DESC);

CREATE TABLE IF NOT EXISTS admin_login_rate (
	id TEXT PRIMARY KEY,
	attempts INTEGER NOT NULL,
	last_fail_at INTEGER NOT NULL
);
