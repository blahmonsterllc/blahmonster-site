import portfolioData from '../../../../data/portfolio.json';

export async function generateStaticParams() {
	return portfolioData.map((project) => ({
		slug: project.slug
	}));
}

export default async function ProjectPage({ params }: { params: Promise<{ slug: string }> }) {
	const { slug } = await params;
	const project = portfolioData.find((p) => p.slug === slug);

	if (!project) {
		return (
			<section>
				<div className="sec-num">/ 404</div>
				<h2 className="sec-title">Project not found</h2>
				<p style={{ maxWidth: '60ch', lineHeight: 1.65 }}>
					That project doesn&apos;t exist (or hasn&apos;t been published yet).
				</p>
				<a href="/portfolio" className="more-link">
					← Back to archive
				</a>
			</section>
		);
	}

	return (
		<article>
			<div className="sec-num">/ {('num' in project && project.num) || 'project'}</div>
			<h2 className="sec-title">
				{project.title}
				<span className="meta">
					{project.category} · {project.year}
				</span>
			</h2>
			{project.description && (
				<p className="hero-body" style={{ marginBottom: 24 }}>
					{project.description}
				</p>
			)}

			{project.url && (
				<div className="cta-row" style={{ marginTop: 8, marginBottom: 32 }}>
					<a
						href={project.url}
						target="_blank"
						rel="noopener noreferrer"
						className="cta solid"
					>
						Visit site →
					</a>
				</div>
			)}

			<hr />

			<dl className="contact-grid" style={{ marginTop: 24 }}>
				<dt>Category</dt>
				<dd>{project.category}</dd>
				<dt>Year</dt>
				<dd>{project.year}</dd>
				{project.url && (
					<>
						<dt>Link</dt>
						<dd>
							<a href={project.url} target="_blank" rel="noopener noreferrer">
								{project.url.replace(/^https?:\/\//, '')}
							</a>
						</dd>
					</>
				)}
			</dl>

			<a href="/portfolio" className="more-link" style={{ marginTop: 40 }}>
				← Back to archive
			</a>
		</article>
	);
}
