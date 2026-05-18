import { NextResponse } from 'next/server';
import { getBlogBindings } from '@/lib/blog/cf';

export const runtime = 'edge';

export async function GET(
	_request: Request,
	ctx: { params: Promise<{ key: string[] }> }
) {
	const { key: segments } = await ctx.params;
	const key = segments.join('/');
	if (!key || key.includes('..')) {
		return new NextResponse('Bad request', { status: 400 });
	}
	const media = getBlogBindings()?.MEDIA;
	if (!media) {
		return new NextResponse('Media not configured', { status: 503 });
	}
	const obj = await media.get(key);
	if (!obj) {
		return new NextResponse('Not found', { status: 404 });
	}
	const headers = new Headers();
	const type = obj.httpMetadata?.contentType ?? 'application/octet-stream';
	headers.set('Content-Type', type);
	headers.set('Cache-Control', 'public, max-age=31536000, immutable');
	return new NextResponse(obj.body, { headers });
}
