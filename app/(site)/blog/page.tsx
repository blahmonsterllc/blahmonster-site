import { getBlogBindings } from '@/lib/blog/cf';
import { getAllPosts } from '@/lib/blog/store';

export const runtime = 'edge';
export const dynamic = 'force-dynamic';

function formatDate(iso: string): string {
	return iso.replace(/-/g, '.');
}

export default async function BlogPage() {
	const db = getBlogBindings()?.DB;
	const posts = await getAllPosts(db);

	return (
		<section>
			<div className="sec-num">/ archive</div>
			<h2 className="sec-title">
				Writing <span className="meta">all posts</span>
			</h2>
			<ol className="post-list" style={{ listStyle: 'none' }}>
				{posts.map((p) => (
					<li key={p.id}>
						<a href={`/blog/${p.slug}`} className="post-row">
							<span className="post-date">{formatDate(p.date)}</span>
							<span className="post-name">{p.title}</span>
							<span className="post-tag">{p.tag ?? 'post'}</span>
						</a>
					</li>
				))}
			</ol>
			<a href="/" className="more-link">
				← Back to home
			</a>
		</section>
	);
}
