import fs from 'fs';
import path from 'path';
import { notFound } from 'next/navigation';

type Post = {
	id: string;
	title: string;
	slug: string;
	date: string;
	excerpt: string;
	content: string;
};

async function getPosts(): Promise<Post[]> {
	const filePath = path.join(process.cwd(), 'data', 'blog.json');
	const fileContents = fs.readFileSync(filePath, 'utf8');
	return JSON.parse(fileContents);
}

export async function generateStaticParams() {
	const posts = await getPosts();
	return posts.map((p) => ({ slug: p.slug }));
}

export default async function PostPage({ params }: { params: { slug: string } }) {
	const posts = await getPosts();
	const post = posts.find((p) => p.slug === params.slug);

	if (!post) {
		notFound();
	}

	return (
		<article style={{maxWidth: 720, margin: '0 auto'}}>
			<header style={{marginBottom: 'var(--spacing-xl)'}}>
				<h1 style={{marginBottom: 'var(--spacing-md)'}}>{post.title}</h1>
				<div style={{
					display: 'flex',
					gap: 12,
					alignItems: 'center',
					fontSize: 15,
					color: 'var(--color-muted)'
				}}>
					<span>📅</span>
					<time>
						{new Date(post.date).toLocaleDateString('en-US', {
							year: 'numeric',
							month: 'long',
							day: 'numeric'
						})}
					</time>
				</div>
			</header>
			<div style={{
				fontSize: 17,
				lineHeight: 1.8,
				color: 'var(--color-text)'
			}}>
				{post.content.split('\n\n').map((paragraph, idx) => (
					<p key={idx} style={{marginBottom: 'var(--spacing-lg)'}}>
						{paragraph}
					</p>
				))}
			</div>
			<footer style={{
				marginTop: 'var(--spacing-2xl)',
				paddingTop: 'var(--spacing-lg)',
				borderTop: '1px solid var(--color-border)',
				textAlign: 'center'
			}}>
				<a href="/blog" style={{color: 'var(--color-accent)', fontWeight: 600}}>
					← Back to Blog
				</a>
			</footer>
		</article>
	);
}
