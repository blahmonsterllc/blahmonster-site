import type { Metadata } from 'next';

export const metadata: Metadata = {
	title: 'TapClick — Privacy Policy',
	description:
		'Privacy policy for TapClick, a metronome app for iPhone and Apple Watch by Blah Monster LLC. TapClick collects no personal data, and health data never leaves your device.',
	robots: { index: true, follow: true }
};

const EFFECTIVE_DATE = 'September 5, 2026';

export default function TapClickPrivacyPage() {
	return (
		<div className="frame legal-page">
			<header className="head">
				<div className="head-left">
					<a href="/legal/tapclick-privacy">
						<span className="domain domain-brand">TapClick</span>
					</a>
				</div>
				<div className="head-right">
					<div>Privacy Policy</div>
					<div>Effective {EFFECTIVE_DATE}</div>
				</div>
			</header>

			<section className="hero" style={{ marginBottom: 48 }}>
				<div className="hero-tag">Metronome for iPhone &amp; Apple Watch</div>
				<h1
					className="hero-title"
					style={{ fontSize: 'clamp(40px, 9vw, 72px)', marginBottom: 28 }}
				>
					Privacy<span className="cursor" />
				</h1>
				<p className="hero-body">
					<strong>Short version:</strong> TapClick does not collect personal data. It has no
					accounts, no analytics, no advertising, no third-party trackers. It works fully
					offline. Your settings stay on your device. On Apple Watch it can log your
					practice to Apple Health — that data stays on your device and is never sent to
					us, and you can switch it off.
				</p>
				<p className="hero-body">
					The long version is below. It exists because the App Store requires every app to
					publish one.
				</p>
				<div className="hero-meta">Last updated: {EFFECTIVE_DATE}</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 01</div>
				<h2 className="sec-title">
					What TapClick does <span className="meta">not collect</span>
				</h2>
				<div className="prose">
					<p>TapClick does not collect, store, transmit, or sell any of the following:</p>
					<ul style={{ paddingLeft: 22, marginBottom: 18 }}>
						<li>Name, email address, phone number, or mailing address.</li>
						<li>Account credentials. There are no accounts in TapClick.</li>
						<li>Location data (precise or approximate).</li>
						<li>Contacts, photos, or calendar entries.</li>
						<li>
							Health data, in the sense of receiving it. The Apple Watch app works with
							Apple Health on your watch, but that data is never transmitted to us or to
							anyone else &mdash; see section 03.
						</li>
						<li>Microphone audio. TapClick does not request microphone access.</li>
						<li>
							Advertising identifiers (IDFA), device fingerprints, or other identifiers used
							for tracking across apps or websites.
						</li>
						<li>Crash logs, analytics, telemetry, or usage statistics.</li>
					</ul>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 02</div>
				<h2 className="sec-title">
					What stays <span className="meta">on your device</span>
				</h2>
				<div className="prose">
					<p>
						TapClick stores your preferences locally on your iPhone. This typically
						includes things like your last-used tempo, time signature, accent pattern, sound
						selection, and haptic settings.
					</p>
					<p>
						This information is stored using standard iOS storage (UserDefaults and the app
						sandbox). It never leaves your device unless you choose to back it up through
						Apple&apos;s iCloud Backup, which is controlled by Apple and not by us. We have
						no access to those backups.
					</p>
					<p>
						Deleting the app from your device removes all locally stored TapClick data.
					</p>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 03</div>
				<h2 className="sec-title">
					Apple Watch <span className="meta">and Apple Health</span>
				</h2>
				<div className="prose">
					<p>
						On Apple Watch, TapClick runs your practice as a workout session. This is what
						keeps the click going while your hands are busy &mdash; without it, watchOS
						suspends the app the moment you drop your arm, which for a metronome is the
						whole ballgame. It also means an hour of playing counts as an hour of work.
					</p>
					<p>
						<strong>What TapClick reads:</strong> your heart rate and active energy while a
						practice session is running, so it can work out how much you burned.
					</p>
					<p>
						<strong>What TapClick writes:</strong> a workout, with its duration and active
						energy, saved to Apple Health on your device when the session ends.
					</p>
					<p>
						<strong>Where it goes:</strong> nowhere. Health data is read from and written to
						Apple Health on your own watch and iPhone. It is never transmitted to Blah
						Monster LLC, never sent to any server, and never shared with any third party.
						We have no way to see it. It is never used for advertising, marketing, or any
						purpose other than showing you your practice session and saving it to Health.
					</p>
					<p>
						<strong>Turning it off:</strong> practice logging is a setting &mdash; Settings
						&rsaquo; Apple Watch &rsaquo; Track Practice Movement in the iPhone app. Switch
						it off and nothing is written to Health; the workout session still runs, purely
						to keep the metronome alive. You can also revoke TapClick&apos;s access
						entirely in the Health app under Sharing &rsaquo; Apps, or on the watch in
						Settings &rsaquo; Privacy &amp; Security &rsaquo; Health.
					</p>
					<p>
						Workouts already saved to Health belong to you and live in the Health app.
						Deleting TapClick does not delete them; you can remove them there at any time.
					</p>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 04</div>
				<h2 className="sec-title">
					Third parties <span className="meta">none</span>
				</h2>
				<div className="prose">
					<p>
						TapClick contains no third-party SDKs for analytics, advertising, attribution,
						crash reporting, or anything else. No data &mdash; health data included &mdash;
						is sent to any server operated by us or by anyone else. The app does not require
						a network connection to function.
					</p>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 05</div>
				<h2 className="sec-title">
					Children&apos;s privacy <span className="meta">COPPA</span>
				</h2>
				<div className="prose">
					<p>
						TapClick is suitable for all ages and does not knowingly collect personal
						information from children under the age of 13. Because TapClick does not collect
						personal information from anyone, this is true by design.
					</p>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 06</div>
				<h2 className="sec-title">
					Your rights <span className="meta">GDPR / CCPA</span>
				</h2>
				<div className="prose">
					<p>
						Because TapClick does not collect or process personal data, there is no personal
						data for us to access, correct, export, or delete on your behalf. The health
						data described in section 03 is held by Apple Health on your own devices, under
						your control, where you can view or delete it directly. If you have any
						concerns about your privacy in connection with TapClick, please contact us
						using the email below.
					</p>
				</div>
			</section>

			<hr />

			<section>
				<div className="sec-num">/ 07</div>
				<h2 className="sec-title">Changes to this policy</h2>
				<div className="prose">
					<p>
						If this policy ever changes in a meaningful way, we will update this page and
						revise the effective date at the top. Material changes will also be noted in the
						release notes of a TapClick update.
					</p>
				</div>
			</section>

			<hr />

			<section id="contact">
				<div className="sec-num">/ 08</div>
				<h2 className="sec-title">Contact</h2>
				<p style={{ marginBottom: 20, maxWidth: '60ch', lineHeight: 1.65 }}>
					Questions about this privacy policy or TapClick in general? Email us. We
					typically reply within two business days.
				</p>
				<dl className="contact-grid">
					<dt>Email</dt>
					<dd>
						<a href="mailto:hello@blahmonster.com?subject=TapClick%20Privacy">
							hello@blahmonster.com
						</a>
					</dd>
					<dt>Publisher</dt>
					<dd>Blah Monster LLC</dd>
					<dt>Address</dt>
					<dd>Long Island, NY, USA</dd>
				</dl>
			</section>

			<footer className="foot">
				<div>© 2026 Blah Monster LLC — TapClick is a product of Blah Monster LLC.</div>
				<div>
					<a href="#">↑ Back to top</a>
				</div>
			</footer>
		</div>
	);
}
