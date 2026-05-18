/// <reference types="@cloudflare/workers-types" />

import { getRequestContext } from '@cloudflare/next-on-pages';

export type BlogBindings = {
	DB?: D1Database;
	MEDIA?: R2Bucket;
};

export function getBlogBindings(): BlogBindings | undefined {
	try {
		const ctx = getRequestContext() as { env: BlogBindings };
		return ctx.env;
	} catch {
		return undefined;
	}
}
