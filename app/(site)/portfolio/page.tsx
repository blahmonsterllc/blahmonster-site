import fs from 'fs';
import path from 'path';

type Project = {
	id: string;
	num?: string;
	title: string;
	slug: string;
	description?: string;
	category: string;
	year: number;
	url?: string;
};

function getProjects(): Project[] {
	const filePath = path.join(process.cwd(), 'data', 'portfolio.json');
	const fileContents = fs.readFileSync(filePath, 'utf8');
	return JSON.parse(fileContents);
}

export default function PortfolioPage() {
	const projects = getProjects();

	return (
		<>
			<section>
				<div className="sec-num">/ archive</div>
				<h2 className="sec-title">
					Work <span className="meta">all projects</span>
				</h2>
				<ol className="work-list">
					{projects.map((p, i) => (
						<li key={p.id} className="work-row">
							<span className="work-num">{p.num ?? String(i + 1).padStart(3, '0')}</span>
							<span className="work-name">
								<a href={p.url ?? `/portfolio/${p.slug}`}
									target={p.url ? '_blank' : undefined}
									rel={p.url ? 'noopener noreferrer' : undefined}
								>
									{p.title}
									{p.url ? ' ↗' : ''}
								</a>
							</span>
							<span className="work-tag">
								{p.category} · {p.year}
							</span>
						</li>
					))}
				</ol>
				<a href="/" className="more-link">
					← Back to home
				</a>
			</section>
		</>
	);
}
