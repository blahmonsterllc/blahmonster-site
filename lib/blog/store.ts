/// <reference types="@cloudflare/workers-types" />

import type { BlogPost } from './types';
import blogFallback from '../../data/blog.json';

export async function getAllPosts(db?: D1Database): Promise<BlogPost[]> {
	if (db) {
		const { results } = await db
			.prepare('SELECT id, title, slug, date, tag, excerpt, content FROM posts ORDER BY date DESC')
			.all();
		return results as BlogPost[];
	}
	const posts = blogFallback as BlogPost[];
	return posts.slice().sort((a, b) => b.date.localeCompare(a.date));
}

export async function getPostBySlug(slug: string, db?: D1Database): Promise<BlogPost | null> {
	if (db) {
		const row = await db
			.prepare('SELECT id, title, slug, date, tag, excerpt, content FROM posts WHERE slug = ?')
			.bind(slug)
			.first();
		return (row as BlogPost) ?? null;
	}
	const posts = blogFallback as BlogPost[];
	return posts.find((p) => p.slug === slug) ?? null;
}

export async function updatePost(
	id: string,
	fields: Partial<Omit<BlogPost, 'id'>>,
	db?: D1Database
): Promise<void> {
	if (!db) throw new Error('no-database');

	const sets: string[] = [];
	const vals: (string | null)[] = [];
	if (fields.title !== undefined) { sets.push('title = ?'); vals.push(fields.title); }
	if (fields.slug !== undefined) { sets.push('slug = ?'); vals.push(fields.slug); }
	if (fields.date !== undefined) { sets.push('date = ?'); vals.push(fields.date); }
	if (fields.tag !== undefined) { sets.push('tag = ?'); vals.push(fields.tag || null); }
	if (fields.excerpt !== undefined) { sets.push('excerpt = ?'); vals.push(fields.excerpt); }
	if (fields.content !== undefined) { sets.push('content = ?'); vals.push(fields.content); }
	if (sets.length === 0) return;

	vals.push(id);
	try {
		const result = await db
			.prepare(`UPDATE posts SET ${sets.join(', ')} WHERE id = ?`)
			.bind(...vals)
			.run();
		if (!result.meta.changes) throw new Error('not-found');
	} catch (e: unknown) {
		const msg = e instanceof Error ? e.message : String(e);
		if (msg === 'not-found') throw e;
		if (msg.includes('UNIQUE') || msg.toLowerCase().includes('unique')) {
			throw new Error('slug-exists');
		}
		throw e;
	}
}

export async function deletePost(id: string, db?: D1Database): Promise<void> {
	if (!db) throw new Error('no-database');
	const result = await db.prepare('DELETE FROM posts WHERE id = ?').bind(id).run();
	if (!result.meta.changes) throw new Error('not-found');
}

export async function insertPost(post: BlogPost, db?: D1Database): Promise<void> {
	if (!db) {
		throw new Error('no-database');
	}
	try {
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
	} catch (e: unknown) {
		const msg = e instanceof Error ? e.message : String(e);
		if (msg.includes('UNIQUE') || msg.toLowerCase().includes('unique')) {
			throw new Error('slug-exists');
		}
		throw e;
	}
}
