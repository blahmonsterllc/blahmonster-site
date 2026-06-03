import { getEnvVar } from './cf';

/**
 * Used for signing sessions and (salted) rate-limit keys. Use a long random string (32+ bytes).
 */
export function getSessionSecret(): string {
	const s = getEnvVar('BLOG_SESSION_SECRET');
	if (!s || s.length < 24) {
		throw new Error('BLOG_SESSION_SECRET must be set (at least 24 random characters).');
	}
	return s;
}
