/// <reference types="@cloudflare/workers-types" />

import { getSessionSecret } from '@/lib/blog/auth-secret';

const WINDOW_SEC = 15 * 60;
const MAX_FAILS = 10;

function getClientIp(request: Request): string {
	const cf = request.headers.get('cf-connecting-ip');
	if (cf) return cf.trim();
	const xff = request.headers.get('x-forwarded-for');
	if (xff) return xff.split(',')[0].trim();
	return 'unknown';
}

async function rateId(ip: string): Promise<string> {
	const enc = new TextEncoder();
	const key = await crypto.subtle.importKey(
		'raw',
		enc.encode(getSessionSecret().slice(0, 32)),
		{ name: 'HMAC', hash: 'SHA-256' },
		false,
		['sign']
	);
	const sig = await crypto.subtle.sign('HMAC', key, enc.encode(`login:${ip}`));
	const hex = [...new Uint8Array(sig)]
		.map((b) => b.toString(16).padStart(2, '0'))
		.join('');
	return hex.slice(0, 40);
}

export async function checkLoginRateLimit(
	request: Request,
	db?: D1Database
): Promise<{ ok: true } | { ok: false; status: 429 }> {
	if (!db) return { ok: true };
	try {
		const ip = getClientIp(request);
		const id = await rateId(ip);
		const now = Math.floor(Date.now() / 1000);
		const row = (await db
			.prepare(
				'SELECT attempts, last_fail_at FROM admin_login_rate WHERE id = ?'
			)
			.bind(id)
			.first()) as { attempts: number; last_fail_at: number } | null;

		if (!row) return { ok: true };

		const elapsed = now - row.last_fail_at;
		let attempts = row.attempts;
		if (elapsed > WINDOW_SEC) {
			attempts = 0;
		}
		if (attempts >= MAX_FAILS) {
			return { ok: false, status: 429 };
		}
		return { ok: true };
	} catch {
		return { ok: true };
	}
}

export async function recordLoginFailure(request: Request, db?: D1Database): Promise<void> {
	if (!db) return;
	try {
		const ip = getClientIp(request);
		const id = await rateId(ip);
		const now = Math.floor(Date.now() / 1000);
		const row = (await db
			.prepare(
				'SELECT attempts, last_fail_at FROM admin_login_rate WHERE id = ?'
			)
			.bind(id)
			.first()) as { attempts: number; last_fail_at: number } | null;

		let attempts = 1;
		if (row) {
			const elapsed = now - row.last_fail_at;
			if (elapsed > WINDOW_SEC) attempts = 1;
			else attempts = row.attempts + 1;
		}

		await db
			.prepare(
				`INSERT INTO admin_login_rate (id, attempts, last_fail_at)
				 VALUES (?, ?, ?)
				 ON CONFLICT(id) DO UPDATE SET
				   attempts = excluded.attempts,
				   last_fail_at = excluded.last_fail_at`
			)
			.bind(id, attempts, now)
			.run();
	} catch {
		/* fail open if table missing / D1 error */
	}
}

export async function clearLoginFailures(request: Request, db?: D1Database): Promise<void> {
	if (!db) return;
	try {
		const ip = getClientIp(request);
		const id = await rateId(ip);
		await db.prepare('DELETE FROM admin_login_rate WHERE id = ?').bind(id).run();
	} catch {
		/* ignore */
	}
}
