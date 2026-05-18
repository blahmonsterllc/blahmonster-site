export function requestIsLocalhost(request: Request): boolean {
	const host = request.headers.get('host') || '';
	return (
		host.startsWith('localhost:') ||
		host === 'localhost' ||
		host.startsWith('127.0.0.1:') ||
		host === '127.0.0.1'
	);
}
