import { sanityClient } from '../../../lib/sanity.client';
import { projectBySlugQuery, projectsQuery } from '../../../lib/sanity.queries';
import { urlFor } from '../../../lib/sanity.image';

export async function generateStaticParams() {
	const projects: any[] = await sanityClient.fetch(projectsQuery);
	return projects.map((p) => ({ slug: p.slug.current }));
}

export default async function ProjectPage({ params }: { params: { slug: string } }) {
	const project = await sanityClient.fetch(projectBySlugQuery, { slug: params.slug });
	if (!project) return <div>Not found</div>;
	return (
		<article>
			<h1>{project.title}</h1>
			{project.description ? <p>{project.description}</p> : null}
			{Array.isArray(project.images) && project.images.length > 0 ? (
				<div style={{display:'grid', gap:12}}>
					{project.images.map((img: any, idx: number) => (
						<img key={idx} src={urlFor(img).width(1200).url()} alt="" style={{width:'100%', height:'auto'}} />
					))}
				</div>
			) : null}
			{project.url ? <p><a href={project.url} target="_blank">Visit</a></p> : null}
		</article>
	);
}
