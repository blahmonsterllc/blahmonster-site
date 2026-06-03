import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/** @type {import('next').NextConfig} */
const adminSecurityHeaders = [
	{ key: 'X-Frame-Options', value: 'DENY' },
	{ key: 'X-Content-Type-Options', value: 'nosniff' },
	{ key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
	{
		key: 'Permissions-Policy',
		value: 'camera=(), microphone=(), geolocation=(), interest-cohort=()'
	},
	{
		key: 'Content-Security-Policy',
		value: [
			"default-src 'self'",
			"base-uri 'self'",
			"form-action 'self'",
			"frame-ancestors 'none'",
			"object-src 'none'",
			"script-src 'self' 'unsafe-inline' 'unsafe-eval'",
			"style-src 'self' 'unsafe-inline'",
			"img-src 'self' data: blob: https:",
			"connect-src 'self'",
			'upgrade-insecure-requests'
		].join('; ')
	}
];

const nextConfig = {
	outputFileTracingRoot: path.join(__dirname),
	async headers() {
		return [
			{ source: '/admin', headers: adminSecurityHeaders },
			{
				source: '/api/admin/:path*',
				headers: [
					{ key: 'X-Content-Type-Options', value: 'nosniff' },
					{ key: 'Cache-Control', value: 'no-store' }
				]
			}
		];
	}
};

export default nextConfig;
