/// <reference types="@cloudflare/workers-types" />

import { getRequestContext } from '@cloudflare/next-on-pages';

export type BlogBindings = {
	DB?: D1Database;
	MEDIA?: R2Bucket;
	BLOG_SESSION_SECRET?: string;
	BLOG_ADMIN_PASSWORD_PBKDF2?: string;
	BLOG_ADMIN_PASSWORD?: string;
};

export function getBlogBindings(): BlogBindings | undefined {
	try {
		const ctx = getRequestContext() as { env: BlogBindings };
		return ctx.env;
	} catch {
		return undefined;
	}
}

export function getEnvVar(name: string): string | undefined {
	try {
		const ctx = getRequestContext() as unknown as { env: Record<string, unknown> };
		const val = ctx.env[name];
		if (typeof val === 'string') return val;
	} catch {}
	return process.env[name] ?? undefined;
}
