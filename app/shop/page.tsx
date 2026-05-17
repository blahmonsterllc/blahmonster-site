import fs from 'fs';
import path from 'path';
import { redirect } from 'next/navigation';

type Product = {
	id: string;
	title: string;
	slug: string;
	price: number;
	stripePriceId: string;
	description: string;
	images: string[];
	category: string;
};

async function getProducts(): Promise<Product[]> {
	const filePath = path.join(process.cwd(), 'data', 'products.json');
	const fileContents = fs.readFileSync(filePath, 'utf8');
	return JSON.parse(fileContents);
}

export default async function ShopPage() {
	const products = await getProducts();

	async function buyNow(formData: FormData) {
		'use server';
		const res = await fetch(`${process.env.NEXT_PUBLIC_SITE_URL}/api/checkout`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({
				priceId: formData.get('priceId'),
				title: formData.get('title'),
				price: Number(formData.get('price'))
			})
		});
		const data = await res.json();
		if (data?.url) {
			redirect(data.url);
		}
	}

	return (
		<section>
			<div style={{marginBottom: 'var(--spacing-2xl)', maxWidth: 720}}>
				<h1>Shop</h1>
				<p style={{fontSize: 18, lineHeight: 1.6}}>
					Original art prints, stickers, buttons, and playful merch. 
					Each piece is made with care and ships worldwide.
				</p>
			</div>

			<div style={{
				display: 'grid',
				gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
				gap: 28
			}}>
				{products.map((p: Product) => (
					<form
						key={p.id}
						action={buyNow}
						className="card"
						style={{
							padding: 0,
							overflow: 'hidden',
							display: 'flex',
							flexDirection: 'column'
						}}
					>
						<div style={{
							height: 280,
							background: 'linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%)',
							display: 'flex',
							alignItems: 'center',
							justifyContent: 'center',
							fontSize: 64,
							borderBottom: '1px solid var(--color-border)'
						}}>
							{p.category === 'sticker' && '🎨'}
							{p.category === 'button' && '📌'}
							{p.category === 'postcard' && '📮'}
							{p.category === 'print' && '🖼️'}
							{!['sticker', 'button', 'postcard', 'print'].includes(p.category) && '🛍️'}
						</div>
						<div style={{padding: 'var(--spacing-lg)', flex: 1, display: 'flex', flexDirection: 'column'}}>
							{p.category && (
								<span style={{
									fontSize: 11,
									color: 'var(--color-muted)',
									textTransform: 'uppercase',
									letterSpacing: '0.08em',
									fontWeight: 600,
									marginBottom: 'var(--spacing-xs)'
								}}>
									{p.category}
								</span>
							)}
							<h3 style={{marginBottom: 'var(--spacing-sm)', fontSize: 20}}>{p.title}</h3>
							<p style={{fontSize: 14, color: 'var(--color-muted)', marginBottom: 'var(--spacing-md)'}}>
								{p.description}
							</p>
							<p style={{
								fontSize: 28,
								fontWeight: 700,
								margin: 'var(--spacing-md) 0',
								color: 'var(--color-text)'
							}}>
								${p.price.toFixed(2)}
							</p>
							<input type="hidden" name="priceId" value={p.stripePriceId || ''} />
							<input type="hidden" name="title" value={p.title} />
							<input type="hidden" name="price" value={p.price} />
							<button type="submit" style={{marginTop: 'auto', width: '100%'}}>
								Add to Cart
							</button>
						</div>
					</form>
				))}
			</div>

			<div style={{
				marginTop: 'var(--spacing-2xl)',
				padding: 'var(--spacing-xl)',
				background: 'var(--color-card-bg)',
				border: '1px solid var(--color-border)',
				borderRadius: 'var(--radius)',
				textAlign: 'center'
			}}>
				<h3 style={{marginBottom: 'var(--spacing-sm)'}}>Add Products</h3>
				<p style={{maxWidth: 560, margin: '0 auto', color: 'var(--color-muted)'}}>
					Edit <code style={{
						padding: '2px 6px',
						background: '#f3f4f6',
						borderRadius: 4,
						fontSize: 14,
						fontFamily: 'monospace'
					}}>data/products.json</code> and add images to <code style={{
						padding: '2px 6px',
						background: '#f3f4f6',
						borderRadius: 4,
						fontSize: 14,
						fontFamily: 'monospace'
					}}>public/shop/</code>
				</p>
			</div>
		</section>
	);
}
