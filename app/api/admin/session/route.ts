import { NextResponse } from 'next/server';
import { getSessionFromRequest } from '@/lib/blog/auth';

export const runtime = 'edge';

export async function GET(request: Request) {
	const ok = await getSessionFromRequest(request);
	return NextResponse.json({ ok });
}
