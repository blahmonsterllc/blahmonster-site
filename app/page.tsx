export default function HomePage() {
	return (
		<>
			{/* Hero Section */}
			<section style={{
				marginBottom: 'var(--spacing-2xl)',
				textAlign: 'center',
				maxWidth: 800,
				margin: '0 auto var(--spacing-2xl)',
				position: 'relative'
			}}>
				<div style={{
					position: 'absolute',
					top: '-40px',
					left: '50%',
					transform: 'translateX(-50%)',
					fontSize: 120,
					opacity: 0.05,
					fontWeight: 900,
					pointerEvents: 'none',
					userSelect: 'none'
				}}>★</div>
				<h1 style={{
					marginBottom: 'var(--spacing-md)',
					position: 'relative'
				}}>
					Creative Lab
				</h1>
				<p style={{
					fontSize: 20,
					lineHeight: 1.6,
					color: 'var(--color-muted)',
					maxWidth: 640,
					margin: '0 auto var(--spacing-lg)'
				}}>
					We craft compelling brand identities, intuitive digital products, and playful experiences 
					that connect with people.
				</p>
				<div style={{
					display: 'flex', 
					gap: 16, 
					flexWrap: 'wrap', 
					justifyContent: 'center',
					marginTop: 'var(--spacing-lg)'
				}}>
					<a href="/portfolio" className="button">View Portfolio</a>
					<a href="/shop" className="button-outline">Shop Art</a>
				</div>
			</section>

			{/* Services Grid */}
			<section style={{
				marginBottom: 'var(--spacing-2xl)',
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
				gap: 'var(--spacing-md)'
			}}>
				<div className="card" style={{
					background: 'linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(168, 85, 247, 0.05) 100%)',
					borderColor: 'rgba(124, 58, 237, 0.2)'
				}}>
					<div style={{
						width: 48,
						height: 48,
						borderRadius: 'var(--radius-sm)',
						background: 'linear-gradient(135deg, #7c3aed 0%, #a855f7 100%)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-md)',
						fontSize: 24
					}}>✨</div>
					<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-sm)'}}>Branding</h3>
					<p style={{margin: 0, fontSize: 15}}>
						Naming, identity systems, guidelines, and visual languages that resonate.
					</p>
				</div>

				<div className="card" style={{
					background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.05) 0%, rgba(8, 145, 178, 0.05) 100%)',
					borderColor: 'rgba(6, 182, 212, 0.2)'
				}}>
					<div style={{
						width: 48,
						height: 48,
						borderRadius: 'var(--radius-sm)',
						background: 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-md)',
						fontSize: 24
					}}>🎨</div>
					<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-sm)'}}>Product Design</h3>
					<p style={{margin: 0, fontSize: 15}}>
						UX/UI, prototypes, design systems, and thoughtful interactions.
					</p>
				</div>

				<div className="card" style={{
					background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.05) 0%, rgba(249, 115, 22, 0.05) 100%)',
					borderColor: 'rgba(245, 158, 11, 0.2)'
				}}>
					<div style={{
						width: 48,
						height: 48,
						borderRadius: 'var(--radius-sm)',
						background: 'linear-gradient(135deg, #f59e0b 0%, #f97316 100%)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-md)',
						fontSize: 24
					}}>⚡</div>
					<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-sm)'}}>Motion & Code</h3>
					<p style={{margin: 0, fontSize: 15}}>
						Microinteractions, animations, and custom development.
					</p>
				</div>
			</section>

			{/* Featured App */}
			<section style={{
				padding: 'var(--spacing-2xl)',
				background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
				borderRadius: 'var(--radius)',
				color: 'white',
				textAlign: 'center',
				position: 'relative',
				overflow: 'hidden'
			}}>
				<div style={{position: 'relative', zIndex: 1}}>
					<div style={{
						display: 'inline-block',
						padding: '6px 16px',
						background: 'rgba(59, 130, 246, 0.2)',
						borderRadius: '100px',
						fontSize: 13,
						fontWeight: 600,
						marginBottom: 'var(--spacing-md)',
						textTransform: 'uppercase',
						letterSpacing: '0.05em',
						border: '1px solid rgba(59, 130, 246, 0.3)'
					}}>Featured App</div>
					<h2 style={{
						fontSize: 'clamp(32px, 5vw, 48px)',
						color: 'white',
						marginBottom: 'var(--spacing-md)'
					}}>TapClick</h2>
					<p style={{
						fontSize: 18,
						maxWidth: 560,
						margin: '0 auto var(--spacing-lg)',
						opacity: 0.95,
						color: 'white'
					}}>
						A minimalist metronome for iOS. Keep perfect time with an intuitive interface designed for musicians.
					</p>
					<a 
						href="/tapclick" 
						className="button"
						style={{
							background: 'white',
							color: '#0f172a',
							boxShadow: '0 8px 24px rgba(0, 0, 0, 0.15)'
						}}
					>
						Learn More →
					</a>
				</div>
				<div style={{
					position: 'absolute',
					top: '-50%',
					right: '-10%',
					width: '60%',
					height: '200%',
					background: 'radial-gradient(circle, rgba(59, 130, 246, 0.15) 0%, transparent 70%)',
					pointerEvents: 'none'
				}} />
			</section>
		</>
	);
}
