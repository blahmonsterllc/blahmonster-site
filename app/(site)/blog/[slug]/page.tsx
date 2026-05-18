import { PostBody } from '@/components/blog/PostBody';
import { getBlogBindings } from '@/lib/blog/cf';
import { getPostBySlug } from '@/lib/blog/store';
import { notFound } from 'next/navigation';

export const runtime = 'edge';
export const dynamic = 'force-dynamic';

export default async function PostPage({ params }: { params: Promise<{ slug: string }> }) {
	const { slug } = await params;
	const db = getBlogBindings()?.DB;
	const post = await getPostBySlug(slug, db);

	if (!post) {
		notFound();
	}

	return (
		<article>
			<div className="sec-num">/ {post.tag?.toLowerCase() ?? 'post'}</div>
			<h2 className="sec-title">
				{post.title}
				<span className="meta">{post.date.replace(/-/g, '.')}</span>
			</h2>

			<div className="prose">
				<PostBody content={post.content} />
			</div>

			<hr />

			<a href="/blog" className="more-link">
				← Back to writing
			</a>
		</article>
	);
}
