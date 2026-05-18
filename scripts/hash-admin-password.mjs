#!/usr/bin/env node
/**
 * Prints BLOG_ADMIN_PASSWORD_PBKDF2 for Cloudflare / .env (never commit real values).
 * Usage: node scripts/hash-admin-password.mjs "your-long-password"
 */
const password = process.argv[2];
if (!password || password.length < 14) {
	console.error('Usage: node scripts/hash-admin-password.mjs "<password at least 14 chars>"');
	process.exit(1);
}

const iterations = 310_000;
const salt = crypto.getRandomValues(new Uint8Array(16));
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
		salt,
		iterations
	},
	keyMaterial,
	256
);

const hash = new Uint8Array(bits);
const saltB64 = Buffer.from(salt).toString('base64');
const hashB64 = Buffer.from(hash).toString('base64');
const record = `pbkdf2$${iterations}$${saltB64}$${hashB64}`;
console.log('');
console.log('Set this as BLOG_ADMIN_PASSWORD_PBKDF2 (single line, no quotes needed in Cloudflare UI):');
console.log(record);
console.log('');
console.log('Remove BLOG_ADMIN_PASSWORD from production after switching.');
console.log('');
