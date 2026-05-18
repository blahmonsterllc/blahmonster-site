import { NextResponse } from 'next/server';
import { getSessionFromRequest } from '@/lib/blog/auth';
import { getBlogBindings } from '@/lib/blog/cf';
import { getAllPosts, insertPost } from '@/lib/blog/store';
import { isValidSlug } from '@/lib/blog/slug';
import type { BlogPost } from '@/lib/blog/types';

export const runtime = 'edge';

export async function GET(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const db = getBlogBindings()?.DB;
	const posts = await getAllPosts(db);
	return NextResponse.json({ posts });
}

export async function POST(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const db = getBlogBindings()?.DB;
	if (!db) {
		return NextResponse.json(
			{
				error:
					'D1 database is not bound. Add a D1 binding named DB to this Pages project, run the SQL schema, then redeploy.'
			},
			{ status: 503 }
		);
	}
	let body: Partial<BlogPost>;
	try {
		body = await request.json();
	} catch {
		return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 });
	}
	const title = String(body.title ?? '').trim();
	const slug = String(body.slug ?? '').trim().toLowerCase();
	const excerpt = String(body.excerpt ?? '').trim();
	const content = String(body.content ?? '');
	const tag = body.tag ? String(body.tag).trim() : '';
	const date = String(body.date ?? '').trim() || new Date().toISOString().slice(0, 10);

	if (!title || !slug || !excerpt) {
		return NextResponse.json({ error: 'title, slug, and excerpt are required' }, { status: 400 });
	}
	if (!isValidSlug(slug)) {
		return NextResponse.json(
			{ error: 'slug must be lowercase letters, numbers, and hyphens only' },
			{ status: 400 }
		);
	}

	const post: BlogPost = {
		id: `post-${crypto.randomUUID()}`,
		title,
		slug,
		date,
		tag: tag || undefined,
		excerpt,
		content
	};

	try {
		await insertPost(post, db);
	} catch (e: unknown) {
		const msg = e instanceof Error ? e.message : String(e);
		if (msg === 'slug-exists') {
			return NextResponse.json({ error: 'A post with this slug already exists' }, { status: 409 });
		}
		return NextResponse.json({ error: msg }, { status: 500 });
	}

	return NextResponse.json({ post });
}
