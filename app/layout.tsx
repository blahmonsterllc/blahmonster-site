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
					padding: '20px 32px',
					borderBottom: '1px solid var(--color-border)',
					background: 'rgba(10, 10, 10, 0.8)',
					backdropFilter: 'blur(20px)',
					WebkitBackdropFilter: 'blur(20px)',
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
								fontWeight: 900, 
								fontSize: 20,
								letterSpacing: '-0.02em',
								color: 'var(--color-text)'
							}}
						>
							<div style={{
								width: 36,
								height: 36,
								borderRadius: '8px',
								background: 'linear-gradient(135deg, var(--color-accent) 0%, var(--color-secondary) 100%)',
								display: 'flex',
								alignItems: 'center',
								justifyContent: 'center',
								fontSize: 20,
								boxShadow: '0 4px 16px rgba(0, 255, 136, 0.3)'
							}}>🦄</div>
							Blahmonster
						</a>
						<div style={{
							marginLeft: 'auto', 
							display: 'flex', 
							gap: 32, 
							fontSize: 15,
							fontWeight: 600
						}}>
							<a href="/portfolio" style={{
								position: 'relative',
								padding: '4px 0',
								color: 'var(--color-muted)',
								transition: 'color 0.3s'
							}}>Portfolio</a>
							<a href="/blog" style={{
								position: 'relative',
								padding: '4px 0',
								color: 'var(--color-muted)',
								transition: 'color 0.3s'
							}}>Blog</a>
							<a href="/shop" style={{
								position: 'relative',
								padding: '4px 0',
								color: 'var(--color-muted)',
								transition: 'color 0.3s'
							}}>Shop</a>
						</div>
					</nav>
				</header>
				<main style={{
					maxWidth: 1280, 
					margin: '0 auto', 
					padding: 'var(--spacing-3xl) 32px', 
					minHeight: 'calc(100vh - 240px)'
				}}>
					{children}
				</main>
				<footer style={{
					padding: 'var(--spacing-2xl) 32px',
					borderTop: '1px solid var(--color-border)',
					background: 'var(--color-surface)',
					marginTop: 'var(--spacing-3xl)'
				}}>
					<div style={{
						maxWidth: 1280,
						margin: '0 auto',
						display: 'grid',
						gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
						gap: 'var(--spacing-xl)'
					}}>
						<div>
							<h4 style={{marginBottom: 16, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-accent)', fontWeight: 700}}>Lab</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 12, fontSize: 15}}>
								<a href="/portfolio" style={{color: 'var(--color-muted)'}}>Portfolio</a>
								<a href="/blog" style={{color: 'var(--color-muted)'}}>Blog</a>
								<a href="/shop" style={{color: 'var(--color-muted)'}}>Shop</a>
							</nav>
						</div>
						<div>
							<h4 style={{marginBottom: 16, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-accent)', fontWeight: 700}}>Apps</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 12, fontSize: 15}}>
								<a href="/tapclick" style={{color: 'var(--color-muted)'}}>TapClick</a>
							</nav>
						</div>
						<div>
							<h4 style={{marginBottom: 16, fontSize: 14, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--color-accent)', fontWeight: 700}}>Connect</h4>
							<nav style={{display: 'flex', flexDirection: 'column', gap: 12, fontSize: 15}}>
								<a href="mailto:hello@blahmonster.com" style={{color: 'var(--color-muted)'}}>Email</a>
							</nav>
						</div>
					</div>
					<div style={{
						maxWidth: 1280,
						margin: 'var(--spacing-xl) auto 0',
						paddingTop: 'var(--spacing-lg)',
						borderTop: '1px solid var(--color-border)',
						textAlign: 'center',
						fontSize: 14,
						color: 'var(--color-muted)'
					}}>
						<p>© {new Date().getFullYear()} Blahmonster Creative Lab. All rights reserved.</p>
					</div>
				</footer>
			</body>
		</html>
	);
}
