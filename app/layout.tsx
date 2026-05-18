import type { Metadata } from 'next';
import { JetBrains_Mono } from 'next/font/google';
import './globals.css';

const jetbrains = JetBrains_Mono({
	subsets: ['latin'],
	variable: '--font-jetbrains',
	display: 'swap',
	weight: ['400', '500', '700', '800']
});

export const metadata: Metadata = {
	title: 'Blah Monster — Art & Software Studio, Long Island NY',
	description:
		'Blah Monster is a one-person studio on Long Island making illustration, web apps, and Mac/iOS software.',
	icons: {
		icon: [{ url: '/favicon.png', type: 'image/png', sizes: '512x512' }],
		apple: [{ url: '/brand/blahmonster-icon.png', type: 'image/png' }]
	},
	openGraph: {
		title: 'Blah Monster',
		description: 'Art & Software Studio. Long Island, NY. Est. 2024.',
		type: 'website'
	}
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
	return (
		<html lang="en" className={jetbrains.variable}>
			<body>{children}</body>
		</html>
	);
}
