import { NextResponse } from 'next/server';
import { getSessionFromRequest } from '@/lib/blog/auth';
import { getBlogBindings } from '@/lib/blog/cf';

export const runtime = 'edge';

export async function POST(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const media = getBlogBindings()?.MEDIA;
	if (!media) {
		return NextResponse.json(
			{
				error:
					'R2 bucket MEDIA is not bound. Create a bucket, bind it as MEDIA on Cloudflare Pages, redeploy, then try again. You can also paste IMAGE:https://... or VIDEO:https://... lines in the editor.'
			},
			{ status: 501 }
		);
	}
	const form = await request.formData();
	const file = form.get('file');
	if (!file || !(file instanceof File) || file.size === 0) {
		return NextResponse.json({ error: 'Expected file field' }, { status: 400 });
	}
	const max = 80 * 1024 * 1024; // 80 MB
	if (file.size > max) {
		return NextResponse.json({ error: 'File too large (max 80MB)' }, { status: 400 });
	}
	const rawName = file.name || 'upload.bin';
	const safe = rawName.replace(/[^a-zA-Z0-9._-]/g, '_').slice(0, 96);
	const key = `b/${crypto.randomUUID()}-${safe}`;
	const ct = file.type || 'application/octet-stream';
	await media.put(key, file.stream(), {
		httpMetadata: { contentType: ct }
	});
	const origin = new URL(request.url).origin;
	const url = `${origin}/api/blog/media/${key}`;
	return NextResponse.json({ url, key });
}
