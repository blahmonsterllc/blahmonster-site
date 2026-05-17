import portfolioData from '../../../data/portfolio.json';

export async function generateStaticParams() {
	return portfolioData.map((project) => ({
		slug: project.slug
	}));
}

export default async function ProjectPage({ params }: { params: Promise<{ slug: string }> }) {
	const { slug } = await params;
	const project = portfolioData.find(p => p.slug === slug);
	
	if (!project) {
		return (
			<div style={{textAlign: 'center', padding: 'var(--spacing-2xl)'}}>
				<h1>Project not found</h1>
			</div>
		);
	}

	return (
		<article style={{maxWidth: 900, margin: '0 auto'}}>
			<div style={{marginBottom: 'var(--spacing-xl)'}}>
				<div style={{
					display: 'inline-block',
					padding: '4px 12px',
					background: 'var(--color-accent)',
					color: 'white',
					borderRadius: '100px',
					fontSize: 12,
					fontWeight: 600,
					textTransform: 'uppercase',
					letterSpacing: '0.05em',
					marginBottom: 'var(--spacing-md)'
				}}>
					{project.category}
				</div>
				<h1 style={{marginBottom: 'var(--spacing-sm)'}}>{project.title}</h1>
				<p style={{fontSize: 18, lineHeight: 1.6, color: 'var(--color-muted)'}}>
					{project.description}
				</p>
				{project.url && (
					<a 
						href={project.url} 
						target="_blank" 
						rel="noopener noreferrer"
						className="button"
						style={{marginTop: 'var(--spacing-md)'}}
					>
						Visit Site →
					</a>
				)}
			</div>

			{project.images && project.images.length > 0 && (
				<div style={{
					display: 'grid',
					gap: 'var(--spacing-lg)',
					marginTop: 'var(--spacing-2xl)'
				}}>
					{project.images.map((img: string, idx: number) => (
						<img 
							key={idx} 
							src={img} 
							alt={`${project.title} screenshot ${idx + 1}`}
							style={{
								width: '100%',
								height: 'auto',
								borderRadius: 'var(--radius)',
								border: '1px solid var(--color-border)'
							}}
						/>
					))}
				</div>
			)}

			<div style={{
				marginTop: 'var(--spacing-2xl)',
				padding: 'var(--spacing-lg)',
				background: 'var(--color-card-bg)',
				border: '1px solid var(--color-border)',
				borderRadius: 'var(--radius)'
			}}>
				<p style={{fontSize: 14, margin: 0}}>
					<strong>Year:</strong> {project.year}
				</p>
			</div>

			<div style={{marginTop: 'var(--spacing-xl)', textAlign: 'center'}}>
				<a href="/portfolio" style={{color: 'var(--color-accent)', fontWeight: 600}}>
					← Back to Portfolio
				</a>
			</div>
		</article>
	);
}
