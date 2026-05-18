import Image from 'next/image';

export default function SiteLayout({ children }: { children: React.ReactNode }) {
	return (
		<div className="frame">
			<header className="head">
				<div className="head-left">
					<a href="/">
						<span className="mark">
							<Image
								src="/brand/blahmonster-logo.svg"
								alt="Blah Monster mark"
								width={40}
								height={40}
								priority
							/>
						</span>
						<span className="domain">blahmonster.com</span>
					</a>
				</div>
				<div className="head-right">
					<div className="status">
						<span className="status-dot" /> Booking late 2026
					</div>
					<div>Long Island, NY / Est. 2024</div>
				</div>
			</header>

			{children}

			<footer className="foot">
				<div>© 2026 Blah Monster LLC / Made on Long Island</div>
				<div>
					<a href="#">↑ Back to top</a>
				</div>
			</footer>
		</div>
	);
}
