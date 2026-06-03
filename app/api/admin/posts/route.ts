import { NextResponse } from 'next/server';
import { getSessionFromRequest } from '@/lib/blog/auth';
import { getBlogBindings } from '@/lib/blog/cf';
import { getAllPosts, insertPost, updatePost, deletePost } from '@/lib/blog/store';
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

export async function PUT(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const db = getBlogBindings()?.DB;
	if (!db) {
		return NextResponse.json({ error: 'D1 not configured' }, { status: 503 });
	}
	let body: Partial<BlogPost> & { id?: string };
	try {
		body = await request.json();
	} catch {
		return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 });
	}
	const id = String(body.id ?? '').trim();
	if (!id) {
		return NextResponse.json({ error: 'id is required' }, { status: 400 });
	}

	const fields: Partial<Omit<BlogPost, 'id'>> = {};
	if (body.title !== undefined) fields.title = String(body.title).trim();
	if (body.slug !== undefined) {
		fields.slug = String(body.slug).trim().toLowerCase();
		if (!isValidSlug(fields.slug)) {
			return NextResponse.json(
				{ error: 'slug must be lowercase letters, numbers, and hyphens only' },
				{ status: 400 }
			);
		}
	}
	if (body.date !== undefined) fields.date = String(body.date).trim();
	if (body.tag !== undefined) fields.tag = String(body.tag).trim();
	if (body.excerpt !== undefined) fields.excerpt = String(body.excerpt).trim();
	if (body.content !== undefined) fields.content = String(body.content);

	try {
		await updatePost(id, fields, db);
	} catch (e: unknown) {
		const msg = e instanceof Error ? e.message : String(e);
		if (msg === 'not-found') {
			return NextResponse.json({ error: 'Post not found' }, { status: 404 });
		}
		if (msg === 'slug-exists') {
			return NextResponse.json({ error: 'A post with this slug already exists' }, { status: 409 });
		}
		return NextResponse.json({ error: msg }, { status: 500 });
	}

	return NextResponse.json({ ok: true });
}

export async function DELETE(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const db = getBlogBindings()?.DB;
	if (!db) {
		return NextResponse.json({ error: 'D1 not configured' }, { status: 503 });
	}
	let body: { id?: string };
	try {
		body = await request.json();
	} catch {
		return NextResponse.json({ error: 'Invalid JSON' }, { status: 400 });
	}
	const id = String(body.id ?? '').trim();
	if (!id) {
		return NextResponse.json({ error: 'id is required' }, { status: 400 });
	}
	try {
		await deletePost(id, db);
	} catch (e: unknown) {
		const msg = e instanceof Error ? e.message : String(e);
		if (msg === 'not-found') {
			return NextResponse.json({ error: 'Post not found' }, { status: 404 });
		}
		return NextResponse.json({ error: msg }, { status: 500 });
	}
	return NextResponse.json({ ok: true });
}
