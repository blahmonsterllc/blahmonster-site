import { getSessionSecret } from './auth-secret';

/** Production (HTTPS): __Host- prefix — no Domain, Path=/, Secure required. */
export const SESSION_COOKIE_PROD = '__Host-bm_blog';

/** Local HTTP dev fallback (no Secure → cannot use __Host-). */
export const SESSION_COOKIE_DEV = 'bm_blog_session';

const MAX_AGE_SEC = 60 * 60 * 24 * 3; // 3 days (shorter exposure window)

async function hmacHex(secret: string, message: string): Promise<string> {
	const enc = new TextEncoder();
	const key = await crypto.subtle.importKey(
		'raw',
		enc.encode(secret),
		{ name: 'HMAC', hash: 'SHA-256' },
		false,
		['sign']
	);
	const sig = await crypto.subtle.sign('HMAC', key, enc.encode(message));
	const bytes = new Uint8Array(sig);
	return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

function timingSafeEqualHex(a: string, b: string): boolean {
	if (a.length !== b.length) return false;
	let out = 0;
	for (let i = 0; i < a.length; i++) {
		out |= a.charCodeAt(i) ^ b.charCodeAt(i);
	}
	return out === 0;
}

export async function makeSessionCookieValue(): Promise<string> {
	const exp = Math.floor(Date.now() / 1000) + MAX_AGE_SEC;
	const nonceBytes = crypto.getRandomValues(new Uint8Array(16));
	const nonce = [...nonceBytes].map((b) => b.toString(16).padStart(2, '0')).join('');
	const body = `v2.${exp}.${nonce}`;
	const sig = await hmacHex(getSessionSecret(), body);
	return `${body}.${sig}`;
}

export async function verifySessionCookie(raw: string | undefined): Promise<boolean> {
	if (!raw) return false;
	const lastDot = raw.lastIndexOf('.');
	if (lastDot <= 0) return false;
	const body = raw.slice(0, lastDot);
	const sig = raw.slice(lastDot + 1);
	const now = Math.floor(Date.now() / 1000);

	const parts = body.split('.');
	if (parts[0] === 'v1' && parts.length === 2) {
		const exp = parseInt(parts[1], 10);
		if (!Number.isFinite(exp) || exp < now) return false;
		const expected = await hmacHex(getSessionSecret(), body);
		return timingSafeEqualHex(sig.toLowerCase(), expected.toLowerCase());
	}
	if (parts[0] === 'v2' && parts.length === 3) {
		const exp = parseInt(parts[1], 10);
		if (!Number.isFinite(exp) || exp < now) return false;
		const expected = await hmacHex(getSessionSecret(), body);
		return timingSafeEqualHex(sig.toLowerCase(), expected.toLowerCase());
	}
	return false;
}

export function parseCookieHeader(header: string | null): Record<string, string> {
	if (!header) return {};
	const out: Record<string, string> = {};
	for (const bit of header.split(';')) {
		const i = bit.indexOf('=');
		if (i === -1) continue;
		const k = bit.slice(0, i).trim();
		const v = bit.slice(i + 1).trim();
		out[k] = decodeURIComponent(v);
	}
	return out;
}

export function getSessionTokenFromCookies(cookies: Record<string, string>): string | undefined {
	return cookies[SESSION_COOKIE_PROD] || cookies[SESSION_COOKIE_DEV];
}

export async function getSessionFromRequest(request: Request): Promise<boolean> {
	const cookies = parseCookieHeader(request.headers.get('cookie'));
	const token = getSessionTokenFromCookies(cookies);
	return verifySessionCookie(token);
}

export function sessionCookieHeaderProd(value: string): string {
	const v = encodeURIComponent(value);
	return `${SESSION_COOKIE_PROD}=${v}; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=${MAX_AGE_SEC}`;
}

export function sessionCookieHeaderDev(value: string): string {
	const v = encodeURIComponent(value);
	return `${SESSION_COOKIE_DEV}=${v}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${MAX_AGE_SEC}`;
}

export function clearSessionCookieHeaders(): string[] {
	return [
		`${SESSION_COOKIE_PROD}=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0`,
		`${SESSION_COOKIE_DEV}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`
	];
}

/** Single header line clearing dev cookie for localhost over HTTP */
export function clearSessionCookieHeaderDevOnly(): string {
	return `${SESSION_COOKIE_DEV}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`;
}

export {
	SESSION_COOKIE_PROD as SESSION_COOKIE_NAME_PROD,
	SESSION_COOKIE_DEV as SESSION_COOKIE_NAME_DEV
};
