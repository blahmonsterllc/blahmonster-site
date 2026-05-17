import fs from 'fs';
import path from 'path';

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

export default async function BlogPage() {
	const posts = await getPosts();

	return (
		<section>
			<div style={{marginBottom: 'var(--spacing-2xl)', maxWidth: 720}}>
				<h1>Blog</h1>
				<p style={{fontSize: 18, lineHeight: 1.6}}>
					Thoughts on design, process, tools, and building products that matter. 
					Occasional musings on creativity and craft.
				</p>
			</div>

			<div style={{maxWidth: 800, display: 'grid', gap: 24}}>
				{posts.map((p) => (
					<a
						key={p.id}
						href={`/blog/${p.slug}`}
						className="card"
						style={{
							display: 'block',
							cursor: 'pointer'
						}}
					>
						<div style={{display: 'flex', gap: 'var(--spacing-md)', alignItems: 'flex-start'}}>
							<div style={{flex: 1}}>
								<h3 style={{marginBottom: 'var(--spacing-sm)', fontSize: 24}}>{p.title}</h3>
								<div style={{
									display: 'flex',
									gap: 12,
									alignItems: 'center',
									marginBottom: 'var(--spacing-sm)',
									fontSize: 14,
									color: 'var(--color-muted)'
								}}>
									<span>📅</span>
									<time>
										{new Date(p.date).toLocaleDateString('en-US', {
											year: 'numeric',
											month: 'long',
											day: 'numeric'
										})}
									</time>
								</div>
								<p style={{
									margin: 0,
									fontSize: 16,
									lineHeight: 1.6,
									display: '-webkit-box',
									WebkitLineClamp: 2,
									WebkitBoxOrient: 'vertical',
									overflow: 'hidden'
								}}>
									{p.excerpt}
								</p>
							</div>
							<div style={{
								fontSize: 24,
								opacity: 0.5,
								transition: 'var(--transition)'
							}}>→</div>
						</div>
					</a>
				))}
			</div>

			<div style={{
				marginTop: 'var(--spacing-2xl)',
				padding: 'var(--spacing-xl)',
				background: 'var(--color-card-bg)',
				border: '1px solid var(--color-border)',
				borderRadius: 'var(--radius)',
				textAlign: 'center'
			}}>
				<h3 style={{marginBottom: 'var(--spacing-sm)'}}>Add Blog Posts</h3>
				<p style={{maxWidth: 560, margin: '0 auto', color: 'var(--color-muted)'}}>
					Edit <code style={{
						padding: '2px 6px',
						background: '#f3f4f6',
						borderRadius: 4,
						fontSize: 14,
						fontFamily: 'monospace'
					}}>data/blog.json</code> to add new posts — no CMS required!
				</p>
			</div>
		</section>
	);
}
