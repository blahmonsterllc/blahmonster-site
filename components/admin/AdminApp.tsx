'use client';

import { useCallback, useEffect, useState } from 'react';
import { slugify } from '@/lib/blog/slug';

type Post = {
	id: string;
	title: string;
	slug: string;
	date: string;
	tag?: string;
	excerpt: string;
	content: string;
};

async function apiJson(r: Response): Promise<Record<string, unknown>> {
	return (await r.json().catch(() => ({}))) as Record<string, unknown>;
}

export function AdminApp() {
	const [authed, setAuthed] = useState<boolean | null>(null);
	const [password, setPassword] = useState('');
	const [err, setErr] = useState('');
	const [posts, setPosts] = useState<Post[]>([]);
	const [editingId, setEditingId] = useState<string | null>(null);
	const [title, setTitle] = useState('');
	const [slug, setSlug] = useState('');
	const [tag, setTag] = useState('');
	const [date, setDate] = useState('');
	const [excerpt, setExcerpt] = useState('');
	const [content, setContent] = useState('');
	const [slugTouched, setSlugTouched] = useState(false);
	const [busy, setBusy] = useState(false);

	useEffect(() => {
		fetch('/api/admin/session')
			.then((r) => r.json())
			.then((d) => setAuthed(!!(d as { ok?: boolean }).ok))
			.catch(() => setAuthed(false));
	}, []);

	const loadPosts = useCallback(async () => {
		const r = await fetch('/api/admin/posts');
		const d = await apiJson(r);
		if (Array.isArray(d.posts)) setPosts(d.posts as Post[]);
	}, []);

	useEffect(() => {
		if (authed) void loadPosts();
	}, [authed, loadPosts]);

	useEffect(() => {
		if (!slugTouched && title && !editingId) setSlug(slugify(title));
	}, [title, slugTouched, editingId]);

	const clearForm = () => {
		setEditingId(null);
		setTitle('');
		setSlug('');
		setTag('');
		setDate('');
		setExcerpt('');
		setContent('');
		setSlugTouched(false);
		setErr('');
	};

	const editPost = (post: Post) => {
		setEditingId(post.id);
		setTitle(post.title);
		setSlug(post.slug);
		setTag(post.tag ?? '');
		setDate(post.date);
		setExcerpt(post.excerpt);
		setContent(post.content);
		setSlugTouched(true);
		setErr('');
		window.scrollTo({ top: 0, behavior: 'smooth' });
	};

	const deletePostById = async (post: Post) => {
		if (!window.confirm(`Delete "${post.title}"? This cannot be undone.`)) return;
		setBusy(true);
		const r = await fetch('/api/admin/posts', {
			method: 'DELETE',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ id: post.id })
		});
		const d = await apiJson(r);
		setBusy(false);
		if (!r.ok) {
			window.alert(typeof d.error === 'string' ? d.error : 'Delete failed');
			return;
		}
		if (editingId === post.id) clearForm();
		await loadPosts();
	};

	const login = async (e: React.FormEvent) => {
		e.preventDefault();
		setErr('');
		const r = await fetch('/api/admin/login', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ password })
		});
		const d = await apiJson(r);
		if (!r.ok) {
			setErr(typeof d.error === 'string' ? d.error : 'Login failed');
			return;
		}
		setAuthed(true);
		setPassword('');
	};

	const logout = async () => {
		await fetch('/api/admin/logout', { method: 'POST' });
		setAuthed(false);
	};

	const uploadFile = useCallback(async (file: File | null) => {
		if (!file) return;
		setBusy(true);
		const fd = new FormData();
		fd.append('file', file);
		const r = await fetch('/api/admin/upload', { method: 'POST', body: fd });
		const d = await apiJson(r);
		setBusy(false);
		if (!r.ok) {
			window.alert(typeof d.error === 'string' ? d.error : 'Upload failed');
			return;
		}
		const url = typeof d.url === 'string' ? d.url : '';
		if (!url) {
			window.alert('Upload response missing URL');
			return;
		}
		const line = file.type.startsWith('video/') ? `VIDEO:${url}` : `IMAGE:${url}`;
		setContent((c) => (c.trim() ? `${c.trim()}\n\n${line}\n\n` : `${line}\n\n`));
	}, []);

	const seedDb = async () => {
		if (
			!window.confirm(
				'Import the five default posts from site data? Only works when the posts table is empty.'
			)
		) {
			return;
		}
		setBusy(true);
		setErr('');
		const r = await fetch('/api/admin/seed', { method: 'POST' });
		const d = await apiJson(r);
		setBusy(false);
		if (!r.ok) {
			setErr(typeof d.error === 'string' ? d.error : 'Seed failed');
			window.alert(typeof d.error === 'string' ? d.error : 'Seed failed');
			return;
		}
		const imported = typeof d.imported === 'number' ? d.imported : 0;
		window.alert(`Imported ${imported} posts.`);
		await loadPosts();
	};

	const save = async (e: React.FormEvent) => {
		e.preventDefault();
		setBusy(true);
		setErr('');

		if (editingId) {
			const r = await fetch('/api/admin/posts', {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ id: editingId, title, slug, tag, excerpt, content, date })
			});
			const d = await apiJson(r);
			setBusy(false);
			if (!r.ok) {
				setErr(typeof d.error === 'string' ? d.error : 'Could not save');
				return;
			}
			clearForm();
			await loadPosts();
		} else {
			const r = await fetch('/api/admin/posts', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					title,
					slug,
					tag,
					excerpt,
					content,
					date: date || new Date().toISOString().slice(0, 10)
				})
			});
			const d = await apiJson(r);
			setBusy(false);
			if (!r.ok) {
				setErr(typeof d.error === 'string' ? d.error : 'Could not save');
				return;
			}
			clearForm();
			await loadPosts();
		}
	};

	if (authed === null) {
		return <p className="hero-body">Checking session…</p>;
	}

	if (!authed) {
		return (
			<section>
				<div className="sec-num">/ admin</div>
				<h1 className="sec-title">Blog</h1>
				<p className="hero-body" style={{ marginBottom: 24 }}>
					Sign in to write and publish blog posts. This page is private and not listed on the
					public site.
				</p>
				<form className="admin-form" onSubmit={login}>
					<label htmlFor="pw">Password</label>
					<input
						id="pw"
						type="password"
						autoComplete="current-password"
						value={password}
						onChange={(e) => setPassword(e.target.value)}
						required
					/>
					{err ? (
						<p style={{ color: 'var(--ink)', marginBottom: 12, fontSize: 14 }}>{err}</p>
					) : null}
					<button type="submit" className="cta solid" disabled={busy}>
						Sign in
					</button>
				</form>
				<p className="hero-body" style={{ marginTop: 32, fontSize: 13, color: 'var(--ink-mute)' }}>
					<a href="/" className="more-link">
						← Back to site
					</a>
				</p>
			</section>
		);
	}

	return (
		<section>
			<div className="sec-num">/ admin</div>
			<h1 className="sec-title">
				{editingId ? 'Edit post' : 'New post'}{' '}
				<span className="meta">{editingId ? 'update' : 'publish'}</span>
			</h1>
			<p className="hero-body" style={{ marginBottom: 24 }}>
				{editingId
					? 'Update the fields below and click Save.'
					: 'Fill in the fields below and click Publish. Separate paragraphs with a blank line.'}{' '}
				Use <strong>Add image</strong> or <strong>Add video</strong> to insert media, or paste an
				image URL on its own line as <code>IMAGE:https://…</code>.
			</p>
			{err ? (
				<p style={{ color: 'var(--ink)', marginBottom: 16, maxWidth: '60ch' }}>{err}</p>
			) : null}
			<form className="admin-form" onSubmit={save}>
				<label htmlFor="title">Title</label>
				<input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />

				<label htmlFor="slug">Slug (URL)</label>
				<input
					id="slug"
					value={slug}
					onChange={(e) => {
						setSlugTouched(true);
						setSlug(e.target.value.toLowerCase());
					}}
					required
					pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
					title="lowercase letters, numbers, hyphens"
				/>

				<label htmlFor="tag">Tag (optional)</label>
				<input id="tag" value={tag} onChange={(e) => setTag(e.target.value)} placeholder="Essay" />

				<label htmlFor="date">Date</label>
				<input
					id="date"
					type="date"
					value={date}
					onChange={(e) => setDate(e.target.value)}
					placeholder={new Date().toISOString().slice(0, 10)}
				/>

				<label htmlFor="excerpt">Excerpt</label>
				<textarea
					id="excerpt"
					rows={3}
					value={excerpt}
					onChange={(e) => setExcerpt(e.target.value)}
					required
				/>

				<label htmlFor="content">Body</label>
				<textarea
					id="content"
					rows={16}
					value={content}
					onChange={(e) => setContent(e.target.value)}
					placeholder={'Paragraphs separated by a blank line.\n\nIMAGE:/api/blog/media/b/... optional caption after URL\n\nVIDEO:https://...'}
				/>

				<div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
					<label className="cta" style={{ cursor: busy ? 'default' : 'pointer', margin: 0 }}>
						<input
							type="file"
							accept="image/*"
							disabled={busy}
							style={{ display: 'none' }}
							onChange={(e) => uploadFile(e.target.files?.[0] ?? null)}
						/>
						{busy ? '…' : 'Add image'}
					</label>
					<label className="cta" style={{ cursor: busy ? 'default' : 'pointer', margin: 0 }}>
						<input
							type="file"
							accept="video/*"
							disabled={busy}
							style={{ display: 'none' }}
							onChange={(e) => uploadFile(e.target.files?.[0] ?? null)}
						/>
						{busy ? '…' : 'Add video'}
					</label>
				</div>

				<div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
					<button type="submit" className="cta solid" disabled={busy}>
						{editingId ? 'Save' : 'Publish'}
					</button>
					{editingId ? (
						<button type="button" className="cta" onClick={clearForm}>
							Cancel edit
						</button>
					) : (
						<button
							type="button"
							className="cta"
							disabled={busy}
							onClick={() => void seedDb()}
						>
							Seed default posts
						</button>
					)}
					<button type="button" className="cta" onClick={() => void logout()}>
						Sign out
					</button>
					<a href="/blog" className="more-link" style={{ alignSelf: 'center' }}>
						View blog →
					</a>
				</div>
			</form>

			{posts.length > 0 && (
				<>
					<h2 className="sec-title" style={{ marginTop: 48 }}>
						Posts <span className="meta">{posts.length}</span>
					</h2>
					<ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
						{posts.map((p) => (
							<li
								key={p.id}
								style={{
									padding: '12px 0',
									borderBottom: '1px solid var(--ink-mute, #333)',
									display: 'flex',
									justifyContent: 'space-between',
									alignItems: 'baseline',
									gap: 12,
									flexWrap: 'wrap'
								}}
							>
								<div style={{ flex: 1, minWidth: 200 }}>
									<strong>{p.title}</strong>
									<span
										className="meta"
										style={{ marginLeft: 8 }}
									>
										{p.date}
										{p.tag ? ` / ${p.tag}` : ''}
									</span>
								</div>
								<div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
									<button
										type="button"
										className="cta"
										style={{ padding: '4px 12px', fontSize: 13 }}
										onClick={() => editPost(p)}
									>
										Edit
									</button>
									<button
										type="button"
										className="cta"
										style={{ padding: '4px 12px', fontSize: 13 }}
										disabled={busy}
										onClick={() => void deletePostById(p)}
									>
										Delete
									</button>
								</div>
							</li>
						))}
					</ul>
				</>
			)}
		</section>
	);
}
