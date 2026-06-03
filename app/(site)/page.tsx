import { getBlogBindings } from '@/lib/blog/cf';
import { getAllPosts } from '@/lib/blog/store';

type Work = {
	num: string;
	name: string;
	tag: string;
	href?: string;
	external?: boolean;
};

const work: Work[] = [
	{ num: '001', name: 'Hank', tag: 'Mac App · 2026', href: '/portfolio/hank' },
	{ num: '002', name: 'Keepset', tag: 'Web · 2026', href: 'https://keepset.io', external: true },
	{
		num: '003',
		name: 'TapClick ↗',
		tag: 'iOS App · 2026',
		href: 'https://apps.apple.com/us/app/tapclick-metronome-watch/id6767588626',
		external: true
	},
	{ num: '004', name: 'Editorial Spots', tag: 'Illustration · 2024' },
	{ num: '005', name: 'Brand Identity', tag: 'Branding · 2024', href: '/portfolio/lepizzapie' },
	{
		num: '006',
		name: 'Amazing Monster Coloring Book Vol 1 ↗',
		tag: 'Print · Amazon · 2023',
		href: 'https://www.amazon.com/Amazing-Monster-Coloring-Book-1/dp/B0BZFD19CK',
		external: true
	}
];

const disciplines = [
	{
		name: 'Illustration',
		desc: 'Editorial spots, character work, mascots, zines. Pen, ink, iPad. B&W preferred.'
	},
	{
		name: 'Web Apps',
		desc: 'Marketing sites, landing pages, full-stack tools. React, Swift, FastAPI — whatever the job needs.'
	},
	{
		name: 'iOS & Mac Apps',
		desc: 'Native Swift, SwiftUI, Apple Silicon, on-device ML. Local-first by default.'
	},
	{
		name: 'Brand & Identity',
		desc: 'Logos, mascots, type systems. Opinions included, free of charge.'
	},
	{
		name: 'Hardware (Selectively)',
		desc: 'PCB design, embedded firmware, STM32. For the right project.'
	}
];

const faqs = [
	{ q: 'Who are you?', a: 'Gordon. Solo studio. Long Island, NY.' },
	{
		q: 'What is Blah Monster?',
		a: "The studio name. Also the mascot's name. I've been drawing monsters since the early 2000s. When I traveled, I'd draw monsters on walls and garbage cans and put stickers up wherever I went."
	},
	{ q: 'What do you charge?', a: "Depends on the project. Tell me what you're building." },
	{
		q: 'Why solo?',
		a: 'By choice. Small means close to the work, less overhead, fewer meetings.'
	},
	{
		q: 'Are you available?',
		a: 'Currently booking late 2026. Earlier slots open up sometimes — ask.'
	},
	{
		q: 'Can you do just illustration / just code?',
		a: 'Yes. Happy to do either alone, or both for the same project.'
	}
];

export const runtime = 'edge';
export const dynamic = 'force-dynamic';

export default async function HomePage() {
	const db = getBlogBindings()?.DB;
	const allPosts = await getAllPosts(db);
	const posts = allPosts.slice(0, 5).map((p) => ({
		date: p.date.replace(/-/g, '.'),
		name: p.title,
		tag: p.tag ?? 'post',
		href: `/blog/${p.slug}`
	}));
	const lastUpdated = allPosts[0]?.date.replace(/-/g, '.') ?? '2026.05.07';

	return (
		<>
			<section className="hero">
				<div className="hero-tag">Art &amp; Software Studio</div>
				<h1 className="hero-title">
					Blah
					<br />
					Monster
					<span className="cursor" />
				</h1>
				<p className="hero-body">
					One-person studio on Long Island, NY. I make <strong>illustration</strong>,{' '}
					<strong>web apps</strong>, <strong>iOS &amp; Mac software</strong>, and the
					occasional piece of hardware. Solo by choice. Small enough to stay close to the
					work.
				</p>
				<p className="hero-body">Run by Gordon. Solo engineer, illustrator, and proud CEO.</p>
				<div className="cta-row">
					<a href="#work" className="cta solid">
						See the work →
					</a>
					<a href="#contact" className="cta">
						Start a project
					</a>
				</div>
				<div className="hero-meta">Last updated: {lastUpdated}</div>
			</section>

			<hr />

			<section id="work">
				<div className="sec-num">/ 01</div>
				<h2 className="sec-title">
					Recent Work <span className="meta">2023—2026</span>
				</h2>
				<ol className="work-list">
					{work.map((w) => (
						<li key={w.num} className="work-row">
							<span className="work-num">{w.num}</span>
							<span className="work-name">
								{w.href ? (
									<a
										href={w.href}
										target={w.external ? '_blank' : undefined}
										rel={w.external ? 'noopener noreferrer' : undefined}
									>
										{w.name}
									</a>
								) : (
									w.name
								)}
							</span>
							<span className="work-tag">{w.tag}</span>
						</li>
					))}
				</ol>
				<a href="/portfolio" className="more-link">
					→ See archive
				</a>
			</section>

			<hr />

			<section id="writing">
				<div className="sec-num">/ 02</div>
				<h2 className="sec-title">
					Writing <span className="meta">recent posts</span>
				</h2>
				<ol className="post-list" style={{ listStyle: 'none' }}>
					{posts.map((p) => (
						<li key={p.date}>
							<a href={p.href} className="post-row">
								<span className="post-date">{p.date}</span>
								<span className="post-name">{p.name}</span>
								<span className="post-tag">{p.tag}</span>
							</a>
						</li>
					))}
				</ol>
				<a href="/blog" className="more-link">
					→ Archive · RSS
				</a>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 03</div>
				<h2 className="sec-title">What I Do</h2>
				{disciplines.map((d) => (
					<div key={d.name} className="disc">
						<div className="disc-name">{d.name}</div>
						<div className="disc-desc">{d.desc}</div>
					</div>
				))}
			</section>

			<div className="monster-block">
				{/* eslint-disable-next-line @next/next/no-img-element */}
				<img
					src="/brand/blahmonster-animated.gif"
					alt="Blah Monster animated mark"
					width={443}
					height={480}
					className="monster-mark"
					decoding="async"
				/>
				<div className="monster-caption">
					FIG.&nbsp;1 — <span className="name">Blah</span>, studio mascot. Grumpy on purpose.
				</div>
			</div>

			<section>
				<div className="sec-num">/ 04</div>
				<h2 className="sec-title">About</h2>
				{faqs.map((f) => (
					<div key={f.q} className="qa">
						<div className="q">{f.q}</div>
						<div className="a">{f.a}</div>
					</div>
				))}
			</section>

			<hr />

			<section id="contact">
				<div className="sec-num">/ 05</div>
				<h2 className="sec-title">Contact</h2>
				<p
					style={{
						marginBottom: 20,
						maxWidth: '60ch',
						lineHeight: 1.65
					}}
				>
					Tell me what you&apos;re building. Include rough scope, timeline, budget if you have
					one. I reply within 2 business days.
				</p>
				<dl className="contact-grid">
					<dt>Email</dt>
					<dd>
						<a href="mailto:hello@blahmonster.com">hello@blahmonster.com</a>
					</dd>
					<dt>Instagram</dt>
					<dd>
						<a href="https://instagram.com/blahmonster" target="_blank" rel="noopener noreferrer">
							@blahmonster
						</a>
					</dd>
					<dt>GitHub</dt>
					<dd>
						<a href="https://github.com/blahmonster" target="_blank" rel="noopener noreferrer">
							github.com/blahmonster
						</a>
					</dd>
					<dt>Location</dt>
					<dd>Long Island, NY</dd>
				</dl>
			</section>
		</>
	);
}
