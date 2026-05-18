import { NextResponse } from 'next/server';
import { clearSessionCookieHeaderDevOnly, clearSessionCookieHeaders } from '@/lib/blog/auth';
import { requestIsLocalhost } from '@/lib/blog/http';

export const runtime = 'edge';

export async function POST(request: Request) {
	const local = requestIsLocalhost(request);
	const res = NextResponse.json({ ok: true });
	if (local) {
		res.headers.set('Set-Cookie', clearSessionCookieHeaderDevOnly());
	} else {
		for (const line of clearSessionCookieHeaders()) {
			res.headers.append('Set-Cookie', line);
		}
	}
	return res;
}
