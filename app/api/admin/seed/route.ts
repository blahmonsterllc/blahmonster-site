import { NextResponse } from 'next/server';
import { getSessionFromRequest } from '@/lib/blog/auth';
import { getBlogBindings } from '@/lib/blog/cf';
import blogFallback from '@/data/blog.json';
import type { BlogPost } from '@/lib/blog/types';

export const runtime = 'edge';

export async function POST(request: Request) {
	if (!(await getSessionFromRequest(request))) {
		return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
	}
	const db = getBlogBindings()?.DB;
	if (!db) {
		return NextResponse.json({ error: 'D1 not configured' }, { status: 503 });
	}
	const row = (await db.prepare('SELECT COUNT(*) as c FROM posts').first()) as { c: number } | null;
	const count = Number(row?.c ?? 0);
	if (count > 0) {
		return NextResponse.json(
			{ error: 'Database already has posts. Refusing to seed (avoid duplicates).' },
			{ status: 409 }
		);
	}
	const posts = blogFallback as BlogPost[];
	for (const post of posts) {
		await db
			.prepare(
				`INSERT INTO posts (id, title, slug, date, tag, excerpt, content)
				 VALUES (?, ?, ?, ?, ?, ?, ?)`
			)
			.bind(
				post.id,
				post.title,
				post.slug,
				post.date,
				post.tag ?? null,
				post.excerpt,
				post.content
			)
			.run();
	}
	return NextResponse.json({ ok: true, imported: posts.length });
}
