const MIN_PLAINTEXT_LEN = 14;

/** Preferred: set BLOG_ADMIN_PASSWORD_PBKDF2 (see scripts/hash-admin-password.mjs). */
export async function verifyAdminPassword(plain: string): Promise<boolean> {
	const hashed = process.env.BLOG_ADMIN_PASSWORD_PBKDF2?.trim();
	if (hashed) {
		return verifyPbkdf2Record(plain, hashed);
	}
	const plainEnv = process.env.BLOG_ADMIN_PASSWORD;
	if (!plainEnv) return false;
	if (plainEnv.length < MIN_PLAINTEXT_LEN) {
		return false;
	}
	return timingSafeEqualUtf8(plain, plainEnv);
}

export function adminPasswordConfigured(): boolean {
	if (process.env.BLOG_ADMIN_PASSWORD_PBKDF2?.trim()) return true;
	const p = process.env.BLOG_ADMIN_PASSWORD;
	return !!p && p.length >= MIN_PLAINTEXT_LEN;
}

function timingSafeEqualUtf8(a: string, b: string): boolean {
	const ea = new TextEncoder().encode(a);
	const eb = new TextEncoder().encode(b);
	if (ea.length !== eb.length) return false;
	let x = 0;
	for (let i = 0; i < ea.length; i++) x |= ea[i] ^ eb[i];
	return x === 0;
}

/** Format: pbkdf2$310000$base64salt$base64hash (32-byte SHA-256 output) */
async function verifyPbkdf2Record(password: string, stored: string): Promise<boolean> {
	const parts = stored.split('$');
	if (parts.length !== 4 || parts[0] !== 'pbkdf2') return false;
	const iterations = parseInt(parts[1], 10);
	if (!Number.isFinite(iterations) || iterations < 100_000) return false;
	let salt: Uint8Array;
	let expected: Uint8Array;
	try {
		salt = Uint8Array.from(atob(parts[2]), (c) => c.charCodeAt(0));
		expected = Uint8Array.from(atob(parts[3]), (c) => c.charCodeAt(0));
	} catch {
		return false;
	}
	if (salt.length < 16 || expected.length !== 32) return false;

	const enc = new TextEncoder();
	const keyMaterial = await crypto.subtle.importKey(
		'raw',
		enc.encode(password),
		'PBKDF2',
		false,
		['deriveBits']
	);
		const bits = await crypto.subtle.deriveBits(
		{
			name: 'PBKDF2',
			hash: 'SHA-256',
			salt: Uint8Array.from(salt),
			iterations
		},
		keyMaterial,
		256
	);
	const out = new Uint8Array(bits);
	if (out.length !== expected.length) return false;
	let diff = 0;
	for (let i = 0; i < out.length; i++) diff |= out[i] ^ expected[i];
	return diff === 0;
}

export function minPlaintextPasswordLength(): number {
	return MIN_PLAINTEXT_LEN;
}
