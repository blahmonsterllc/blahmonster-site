'use client';

import { useCallback, useEffect, useState } from 'react';
import { slugify } from '@/lib/blog/slug';

async function apiJson(r: Response): Promise<Record<string, unknown>> {
	return (await r.json().catch(() => ({}))) as Record<string, unknown>;
}

export function AdminApp() {
	const [authed, setAuthed] = useState<boolean | null>(null);
	const [password, setPassword] = useState('');
	const [err, setErr] = useState('');
	const [title, setTitle] = useState('');
	const [slug, setSlug] = useState('');
	const [tag, setTag] = useState('');
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

	useEffect(() => {
		if (!slugTouched && title) setSlug(slugify(title));
	}, [title, slugTouched]);

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
		window.location.href = '/blog';
	};

	const publish = async (e: React.FormEvent) => {
		e.preventDefault();
		setBusy(true);
		setErr('');
		const r = await fetch('/api/admin/posts', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({
				title,
				slug,
				tag,
				excerpt,
				content,
				date: new Date().toISOString().slice(0, 10)
			})
		});
		const d = await apiJson(r);
		setBusy(false);
		if (!r.ok) {
			setErr(typeof d.error === 'string' ? d.error : 'Could not save');
			return;
		}
		window.location.href = `/blog/${encodeURIComponent(slug)}`;
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
				Sign in to publish posts. Use a strong password and prefer{' '}
				<code>BLOG_ADMIN_PASSWORD_PBKDF2</code> in production (see{' '}
				<code>npm run hash-admin-password</code>). This area is not indexed by search engines.
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
				New post <span className="meta">publish</span>
			</h1>
			<p className="hero-body" style={{ marginBottom: 24 }}>
				Uses your live D1 database on Cloudflare. Media uploads need an R2 bucket bound as{' '}
				<code>MEDIA</code>; otherwise paste <code>IMAGE:https://…</code> or{' '}
				<code>VIDEO:https://…</code> on their own lines in the body (blank line between blocks).
			</p>
			{err ? (
				<p style={{ color: 'var(--ink)', marginBottom: 16, maxWidth: '60ch' }}>{err}</p>
			) : null}
			<form className="admin-form" onSubmit={publish}>
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
						Publish
					</button>
					<button
						type="button"
						className="cta"
						disabled={busy}
						onClick={() => void seedDb()}
					>
						Seed default posts
					</button>
					<button type="button" className="cta" onClick={() => void logout()}>
						Sign out
					</button>
					<a href="/blog" className="more-link" style={{ alignSelf: 'center' }}>
						View blog →
					</a>
				</div>
			</form>
		</section>
	);
}
