import type { Metadata } from 'next';

export const metadata: Metadata = {
	title: 'Admin — Blah Monster',
	robots: { index: false, follow: false }
};

export default function AdminLayout({ children }: { children: React.ReactNode }) {
	return <div className="frame admin-shell">{children}</div>;
}
