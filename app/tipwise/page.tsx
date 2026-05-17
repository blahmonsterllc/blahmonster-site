export default function TipwisePage() {
	return (
		<section style={{maxWidth: 800, margin: '0 auto'}}>
			{/* Hero */}
			<div style={{textAlign: 'center', marginBottom: 'var(--spacing-2xl)'}}>
				<div style={{
					width: 120,
					height: 120,
					margin: '0 auto var(--spacing-lg)',
					borderRadius: 24,
					background: 'linear-gradient(135deg, #f59e0b 0%, #f97316 100%)',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					fontSize: 64,
					boxShadow: '0 12px 32px rgba(245, 158, 11, 0.3)'
				}}>
					🦉
				</div>
				<h1>Tipwise</h1>
				<p style={{
					fontSize: 20,
					lineHeight: 1.6,
					maxWidth: 600,
					margin: '0 auto var(--spacing-lg)'
				}}>
					The smart tipping calculator for iOS. Calculate tips instantly, save receipts, 
					and track your dining spending habits—all in one beautiful app.
				</p>
				<div style={{
					display: 'inline-block',
					padding: '8px 20px',
					background: 'rgba(16, 185, 129, 0.1)',
					borderRadius: '100px',
					fontSize: 14,
					fontWeight: 600,
					color: '#10b981'
				}}>
					Coming Soon to iOS
				</div>
			</div>

			{/* Features */}
			<div style={{
				background: 'var(--color-card-bg)',
				border: '1px solid var(--color-border)',
				borderRadius: 'var(--radius)',
				padding: 'var(--spacing-2xl)',
				marginBottom: 'var(--spacing-xl)'
			}}>
				<h2 style={{fontSize: 28, marginBottom: 'var(--spacing-lg)', textAlign: 'center'}}>Features</h2>
				<div style={{display: 'grid', gap: 'var(--spacing-lg)'}}>
					<div style={{display: 'flex', gap: 'var(--spacing-md)', alignItems: 'flex-start'}}>
						<div style={{
							width: 56,
							height: 56,
							borderRadius: 'var(--radius-sm)',
							background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 28,
							flexShrink: 0
						}}>🧮</div>
						<div>
							<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-xs)'}}>Smart Tip Calculator</h3>
							<p style={{margin: 0, fontSize: 16}}>
								Instantly calculate tips with customizable percentages. Split bills effortlessly among friends.
							</p>
						</div>
					</div>

					<div style={{display: 'flex', gap: 'var(--spacing-md)', alignItems: 'flex-start'}}>
						<div style={{
							width: 56,
							height: 56,
							borderRadius: 'var(--radius-sm)',
							background: 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 28,
							flexShrink: 0
						}}>🧾</div>
						<div>
							<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-xs)'}}>Save Receipts</h3>
							<p style={{margin: 0, fontSize: 16}}>
								Capture and store receipts for every meal. Keep everything organized in one place.
							</p>
						</div>
					</div>

					<div style={{display: 'flex', gap: 'var(--spacing-md)', alignItems: 'flex-start'}}>
						<div style={{
							width: 56,
							height: 56,
							borderRadius: 'var(--radius-sm)',
							background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 28,
							flexShrink: 0
						}}>📊</div>
						<div>
							<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-xs)'}}>Track Spending</h3>
							<p style={{margin: 0, fontSize: 16}}>
								See how much you've spent at each restaurant or cuisine type. Export reports anytime.
							</p>
						</div>
					</div>

					<div style={{display: 'flex', gap: 'var(--spacing-md)', alignItems: 'flex-start'}}>
						<div style={{
							width: 56,
							height: 56,
							borderRadius: 'var(--radius-sm)',
							background: 'linear-gradient(135deg, #f59e0b 0%, #f97316 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 28,
							flexShrink: 0
						}}>📤</div>
						<div>
							<h3 style={{fontSize: 20, marginBottom: 'var(--spacing-xs)'}}>Export & Share</h3>
							<p style={{margin: 0, fontSize: 16}}>
								Export your dining history and receipts. Perfect for expense reports or budgeting.
							</p>
						</div>
					</div>
				</div>
			</div>

			{/* Use Cases */}
			<div style={{
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
				gap: 'var(--spacing-md)',
				marginBottom: 'var(--spacing-xl)'
			}}>
				<div className="card" style={{textAlign: 'center'}}>
					<div style={{fontSize: 40, marginBottom: 'var(--spacing-sm)'}}>🍽️</div>
					<h4 style={{fontSize: 18, marginBottom: 'var(--spacing-xs)'}}>Dining Out</h4>
					<p style={{margin: 0, fontSize: 14}}>
						Never struggle with tip math again
					</p>
				</div>
				<div className="card" style={{textAlign: 'center'}}>
					<div style={{fontSize: 40, marginBottom: 'var(--spacing-sm)'}}>👥</div>
					<h4 style={{fontSize: 18, marginBottom: 'var(--spacing-xs)'}}>Split Bills</h4>
					<p style={{margin: 0, fontSize: 14}}>
						Divide costs fairly among friends
					</p>
				</div>
				<div className="card" style={{textAlign: 'center'}}>
					<div style={{fontSize: 40, marginBottom: 'var(--spacing-sm)'}}>💼</div>
					<h4 style={{fontSize: 18, marginBottom: 'var(--spacing-xs)'}}>Expense Tracking</h4>
					<p style={{margin: 0, fontSize: 14}}>
						Track business meals and export reports
					</p>
				</div>
			</div>

			{/* CTA */}
			<div style={{
				textAlign: 'center',
				padding: 'var(--spacing-2xl)',
				background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
				borderRadius: 'var(--radius)',
				color: 'white',
				position: 'relative',
				overflow: 'hidden'
			}}>
				<div style={{position: 'relative', zIndex: 1}}>
					<h3 style={{color: 'white', marginBottom: 'var(--spacing-md)', fontSize: 32}}>
						Join the Waitlist
					</h3>
					<p style={{
						marginBottom: 'var(--spacing-lg)',
						opacity: 0.95,
						fontSize: 17,
						color: 'white',
						maxWidth: 520,
						margin: '0 auto var(--spacing-lg)'
					}}>
						Be the first to get Tipwise when we launch on the App Store. 
						We'll send you exclusive early access and updates.
					</p>
					<a 
						href="#" 
						className="button" 
						style={{
							background: 'white',
							color: '#10b981',
							boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)'
						}}
					>
						Notify Me at Launch →
					</a>
				</div>
				<div style={{
					position: 'absolute',
					top: '-50%',
					right: '-20%',
					width: '70%',
					height: '200%',
					background: 'radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%)',
					pointerEvents: 'none'
				}} />
			</div>
		</section>
	);
}
