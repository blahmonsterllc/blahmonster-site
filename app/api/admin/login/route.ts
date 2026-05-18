import { NextResponse } from 'next/server';
import {
	makeSessionCookieValue,
	sessionCookieHeaderDev,
	sessionCookieHeaderProd
} from '@/lib/blog/auth';
import { getBlogBindings } from '@/lib/blog/cf';
import { requestIsLocalhost } from '@/lib/blog/http';
import { checkLoginRateLimit, clearLoginFailures, recordLoginFailure } from '@/lib/blog/login-rate';
import { adminPasswordConfigured, verifyAdminPassword } from '@/lib/blog/password';

export const runtime = 'edge';

async function stall(ms: number) {
	await new Promise((r) => setTimeout(r, ms));
}

export async function POST(request: Request) {
	const local = requestIsLocalhost(request);
	const db = getBlogBindings()?.DB;

	if (!adminPasswordConfigured()) {
		await stall(450);
		return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
	}

	const limit = await checkLoginRateLimit(request, db);
	if (!limit.ok) {
		await stall(450);
		return NextResponse.json(
			{ error: 'Too many attempts. Try again in about 15 minutes.' },
			{ status: 429 }
		);
	}

	let body: { password?: string };
	try {
		body = await request.json();
	} catch {
		await stall(350);
		return NextResponse.json({ error: 'Invalid request' }, { status: 400 });
	}

	const password = body.password ?? '';
	let ok = false;
	try {
		ok = await verifyAdminPassword(password);
	} catch {
		ok = false;
	}

	if (!ok) {
		await recordLoginFailure(request, db);
		await stall(450 + Math.floor(Math.random() * 200));
		return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
	}

	await clearLoginFailures(request, db);

	try {
		const token = await makeSessionCookieValue();
		const res = NextResponse.json({ ok: true });
		res.headers.set(
			'Set-Cookie',
			local ? sessionCookieHeaderDev(token) : sessionCookieHeaderProd(token)
		);
		return res;
	} catch {
		await stall(300);
		return NextResponse.json({ error: 'Server misconfiguration' }, { status: 503 });
	}
}
