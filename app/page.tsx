export default function HomePage() {
	return (
		<>
			{/* Hero Section */}
			<section style={{
				marginBottom: 'var(--spacing-3xl)',
				textAlign: 'center',
				position: 'relative',
				paddingTop: 'var(--spacing-2xl)'
			}}>
				<div style={{
					position: 'absolute',
					top: '50%',
					left: '50%',
					transform: 'translate(-50%, -50%)',
					width: '600px',
					height: '600px',
					background: 'radial-gradient(circle, rgba(0, 255, 136, 0.15) 0%, transparent 70%)',
					filter: 'blur(80px)',
					pointerEvents: 'none',
					zIndex: 0
				}} />
				<div style={{position: 'relative', zIndex: 1}}>
					<div style={{
						display: 'inline-block',
						padding: '8px 20px',
						background: 'rgba(0, 255, 136, 0.1)',
						border: '1px solid rgba(0, 255, 136, 0.3)',
						borderRadius: '100px',
						fontSize: 13,
						fontWeight: 700,
						color: 'var(--color-accent)',
						marginBottom: 'var(--spacing-lg)',
						textTransform: 'uppercase',
						letterSpacing: '0.1em'
					}}>
						● Blahmonster
					</div>
					<h1 style={{
						marginBottom: 'var(--spacing-lg)',
						maxWidth: 900,
						margin: '0 auto var(--spacing-lg)'
					}}>
						Creative Lab
					</h1>
					<p style={{
						fontSize: 22,
						lineHeight: 1.6,
						color: 'var(--color-muted)',
						maxWidth: 640,
						margin: '0 auto var(--spacing-xl)'
					}}>
						We craft compelling brand identities, intuitive digital products, and playful experiences 
						that connect with people.
					</p>
					<div style={{
						display: 'flex', 
						gap: 20, 
						flexWrap: 'wrap', 
						justifyContent: 'center'
					}}>
						<a href="/portfolio" className="button">View Portfolio →</a>
						<a href="/shop" className="button-outline">Shop Art</a>
					</div>
				</div>
			</section>

			{/* Services Grid */}
			<section style={{
				marginBottom: 'var(--spacing-3xl)',
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
				gap: 'var(--spacing-lg)'
			}}>
				<div className="card">
					<div style={{
						width: 56,
						height: 56,
						borderRadius: 'var(--radius)',
						background: 'linear-gradient(135deg, rgba(0, 255, 136, 0.2) 0%, rgba(0, 255, 136, 0.05) 100%)',
						border: '1px solid rgba(0, 255, 136, 0.3)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-lg)',
						fontSize: 28
					}}>✨</div>
					<h3 style={{fontSize: 24, marginBottom: 'var(--spacing-sm)', color: 'var(--color-text)'}}>Branding</h3>
					<p style={{margin: 0, fontSize: 16, lineHeight: 1.6}}>
						Naming, identity systems, guidelines, and visual languages that resonate.
					</p>
				</div>

				<div className="card">
					<div style={{
						width: 56,
						height: 56,
						borderRadius: 'var(--radius)',
						background: 'linear-gradient(135deg, rgba(255, 51, 102, 0.2) 0%, rgba(255, 51, 102, 0.05) 100%)',
						border: '1px solid rgba(255, 51, 102, 0.3)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-lg)',
						fontSize: 28
					}}>🎨</div>
					<h3 style={{fontSize: 24, marginBottom: 'var(--spacing-sm)', color: 'var(--color-text)'}}>Product Design</h3>
					<p style={{margin: 0, fontSize: 16, lineHeight: 1.6}}>
						UX/UI, prototypes, design systems, and thoughtful interactions.
					</p>
				</div>

				<div className="card">
					<div style={{
						width: 56,
						height: 56,
						borderRadius: 'var(--radius)',
						background: 'linear-gradient(135deg, rgba(0, 255, 136, 0.2) 0%, rgba(255, 51, 102, 0.1) 100%)',
						border: '1px solid rgba(0, 255, 136, 0.3)',
						display: 'flex',
						alignItems: 'center',
						justifyContent: 'center',
						marginBottom: 'var(--spacing-lg)',
						fontSize: 28
					}}>⚡</div>
					<h3 style={{fontSize: 24, marginBottom: 'var(--spacing-sm)', color: 'var(--color-text)'}}>Motion & Code</h3>
					<p style={{margin: 0, fontSize: 16, lineHeight: 1.6}}>
						Microinteractions, animations, and custom development.
					</p>
				</div>
			</section>

			{/* Featured App */}
			<section style={{
				padding: 'var(--spacing-3xl) var(--spacing-2xl)',
				background: 'linear-gradient(135deg, #1a1a1a 0%, #0f0f0f 100%)',
				borderRadius: 'var(--radius-lg)',
				border: '1px solid var(--color-border)',
				color: 'white',
				textAlign: 'center',
				position: 'relative',
				overflow: 'hidden',
				boxShadow: '0 20px 60px rgba(0, 0, 0, 0.5)'
			}}>
				{/* Gradient orbs */}
				<div style={{
					position: 'absolute',
					top: '-20%',
					left: '-10%',
					width: '40%',
					height: '100%',
					background: 'radial-gradient(circle, rgba(0, 255, 136, 0.1) 0%, transparent 70%)',
					filter: 'blur(60px)',
					pointerEvents: 'none'
				}} />
				<div style={{
					position: 'absolute',
					bottom: '-20%',
					right: '-10%',
					width: '40%',
					height: '100%',
					background: 'radial-gradient(circle, rgba(255, 51, 102, 0.1) 0%, transparent 70%)',
					filter: 'blur(60px)',
					pointerEvents: 'none'
				}} />
				
				<div style={{position: 'relative', zIndex: 1}}>
					<div style={{
						display: 'inline-block',
						padding: '8px 20px',
						background: 'rgba(0, 255, 136, 0.15)',
						border: '1px solid rgba(0, 255, 136, 0.3)',
						borderRadius: '100px',
						fontSize: 12,
						fontWeight: 700,
						marginBottom: 'var(--spacing-lg)',
						textTransform: 'uppercase',
						letterSpacing: '0.1em',
						color: 'var(--color-accent)'
					}}>Featured App</div>
					<h2 style={{
						fontSize: 'clamp(40px, 6vw, 64px)',
						color: 'white',
						marginBottom: 'var(--spacing-md)',
						fontWeight: 900,
						letterSpacing: '-0.03em'
					}}>TapClick</h2>
					<p style={{
						fontSize: 20,
						maxWidth: 600,
						margin: '0 auto var(--spacing-xl)',
						opacity: 0.9,
						color: 'var(--color-muted)',
						lineHeight: 1.6
					}}>
						A minimalist metronome for iOS. Keep perfect time with an intuitive interface designed for musicians.
					</p>
					<a 
						href="/tapclick" 
						className="button"
						style={{
							background: 'var(--color-accent)',
							color: 'var(--color-bg)',
							boxShadow: '0 8px 32px rgba(0, 255, 136, 0.3)'
						}}
					>
						Learn More →
					</a>
				</div>
			</section>
		</>
	);
}
