import fs from 'fs';
import path from 'path';
import Image from 'next/image';

type Project = {
	id: string;
	title: string;
	slug: string;
	description?: string;
	images: string[];
	category: string;
	year: number;
};

async function getProjects(): Promise<Project[]> {
	const filePath = path.join(process.cwd(), 'data', 'portfolio.json');
	const fileContents = fs.readFileSync(filePath, 'utf8');
	return JSON.parse(fileContents);
}

export default async function PortfolioPage() {
	const projects = await getProjects();

	return (
		<section>
			<div style={{marginBottom: 'var(--spacing-2xl)', maxWidth: 720}}>
				<h1>Portfolio</h1>
				<p style={{fontSize: 18, lineHeight: 1.6}}>
					Selected work across branding, product design, and digital experiences. 
					Each project tells a unique story of collaboration and craft.
				</p>
			</div>

			<div style={{
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
				gap: 32
			}}>
				{projects.map((p) => (
					<a
						key={p.id}
						href={`/portfolio/${p.slug}`}
						className="card"
						style={{
							padding: 0,
							overflow: 'hidden',
							cursor: 'pointer'
						}}
					>
						<div style={{
							height: 240,
							background: 'linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 48,
							borderBottom: '1px solid var(--color-border)',
							position: 'relative',
							overflow: 'hidden'
						}}>
							{p.category === 'branding' && '✨'}
							{p.category === 'product' && '📱'}
							{p.category === 'web' && '🌐'}
							{!['branding', 'product', 'web'].includes(p.category) && '🎨'}
						</div>
						<div style={{padding: 'var(--spacing-lg)'}}>
							<div style={{
								display: 'flex',
								justifyContent: 'space-between',
								alignItems: 'center',
								marginBottom: 'var(--spacing-xs)'
							}}>
								<span style={{
									fontSize: 11,
									textTransform: 'uppercase',
									letterSpacing: '0.08em',
									color: 'var(--color-muted)',
									fontWeight: 600
								}}>
									{p.category}
								</span>
								<span style={{fontSize: 13, color: 'var(--color-muted)'}}>
									{p.year}
								</span>
							</div>
							<h3 style={{marginBottom: 'var(--spacing-xs)', fontSize: 22}}>{p.title}</h3>
							{p.description && (
								<p style={{
									margin: 0,
									fontSize: 15,
									lineHeight: 1.6,
									display: '-webkit-box',
									WebkitLineClamp: 2,
									WebkitBoxOrient: 'vertical',
									overflow: 'hidden'
								}}>
									{p.description}
								</p>
							)}
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
				<h3 style={{marginBottom: 'var(--spacing-sm)'}}>Add Your Projects</h3>
				<p style={{maxWidth: 560, margin: '0 auto var(--spacing-md)', color: 'var(--color-muted)'}}>
					Edit <code style={{
						padding: '2px 6px',
						background: '#f3f4f6',
						borderRadius: 4,
						fontSize: 14,
						fontFamily: 'monospace'
					}}>data/portfolio.json</code> and add images to <code style={{
						padding: '2px 6px',
						background: '#f3f4f6',
						borderRadius: 4,
						fontSize: 14,
						fontFamily: 'monospace'
					}}>public/portfolio/</code>
				</p>
			</div>
		</section>
	);
}
