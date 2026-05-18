export function slugify(title: string): string {
	const s = title
		.toLowerCase()
		.trim()
		.replace(/['']/g, '')
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/^-+|-+$/g, '');
	return s || 'post';
}

export function isValidSlug(slug: string): boolean {
	return /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug);
}
