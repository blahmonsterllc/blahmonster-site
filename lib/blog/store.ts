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
