import Image from 'next/image';
import './globals.css';

export const metadata = {
	title: 'Blahmonster — Creative Lab',
	description: 'Portfolio, apps, blog, and shop by Blahmonster creative lab.',
	icons: {
		icon: '/brand/blahmonster-logo.svg'
	}
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
	return (
		<html lang="en">
			<body>
				<header style={{
					padding: '24px 32px',
					borderBottom: '1px solid var(--color-border)',
					background: 'rgba(255, 255, 255, 0.8)',
					backdropFilter: 'blur(12px)',
					WebkitBackdropFilter: 'blur(12px)',
					position: 'sticky',
					top: 0,
					zIndex: 100
				}}>
					<nav style={{
						maxWidth: 1280,
						margin: '0 auto',
						display: 'flex',
						gap: 40,
						alignItems: 'center'
					}}>
						<a 
							href="/" 
							style={{
								display: 'flex', 
								alignItems: 'center', 
								gap: 12, 
								fontWeight: 700, 
								fontSize: 20,
								letterSpacing: '-0.02em'
							}}
						>
							<Image 
								src="/brand/blahmonster-logo.svg" 
								alt="Blahmonster" 
								width={36} 
								height={36} 
								priority 
								style={{
									filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
								}}
							/>
							Blahmonster
						</a>
						<div style={{
							marginLeft: 'auto', 
							display: 'flex', 
							gap: 32, 
							fontSize: 15,
							fontWeight: 500
						}}>
							<a href="/portfolio" style={{
								position: 'relative',
								padding: '4px 0'
							}}>Portfolio</a>
							<a href="/blog" style={{
								position: 'relative',
								padding: '4px 0'
							}}>Blog</a>
							<a href="/shop" style={{
								position: 'relative',
								padding: '4px 0'
							}}>Shop</a>
						</div>
					</nav>
				</header>
				<main style={{
					maxWidth: 1280, 
					margin: '0 auto', 
					padding: 'var(--spacing-2xl) 32px', 
					minHeight: 'calc(100vh - 240px)'
				}}>
					{children}
				</main>
				<footer style={{
					padding: 'var(--spacing-xl) 32px',
					borderTop: '1px solid var(--color-border)',
					background: 'var(--color-card-bg)',
					marginTop: 'var(--spacing-2xl)'
				}}>
					<div style={{
						maxWidth: 1280,
						margin: '0 auto',
						display: 'grid',
						gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
						gap: 'var(--spacing-lg)'
					}}>
						<div>
							<h4 style={{marginBottom: 12, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-muted)'}}>Lab</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 8, fontSize: 14}}>
								<a href="/portfolio">Portfolio</a>
								<a href="/blog">Blog</a>
								<a href="/shop">Shop</a>
							</nav>
						</div>
						<div>
							<h4 style={{marginBottom: 12, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-muted)'}}>Apps</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 8, fontSize: 14}}>
								<a href="/tipwise">Tipwise</a>
							</nav>
						</div>
						<div>
							<h4 style={{marginBottom: 12, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-muted)'}}>Connect</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 8, fontSize: 14}}>
								<a href="mailto:hello@blahmonster.com">Email</a>
							</nav>
						</div>
					</div>
					<div style={{
						maxWidth: 1280,
						margin: 'var(--spacing-lg) auto 0',
						paddingTop: 'var(--spacing-lg)',
						borderTop: '1px solid var(--color-border)',
						textAlign: 'center',
						fontSize: 13,
						color: 'var(--color-muted)'
					}}>
						<p>© {new Date().getFullYear()} Blahmonster Creative Lab. All rights reserved.</p>
					</div>
				</footer>
			</body>
		</html>
	);
}
