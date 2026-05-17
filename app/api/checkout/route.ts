import Stripe from 'stripe';
import { NextResponse } from 'next/server';

export const runtime = 'edge';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY as string, { apiVersion: '2023-10-16' });

export async function POST(request: Request) {
	try {
		const { priceId, title, price } = await request.json();
		const lineItems = priceId ? [ { price: priceId, quantity: 1 } ] : [ { price_data: { currency: 'usd', product_data: { name: title }, unit_amount: Math.round(Number(price) * 100) }, quantity: 1 } ];
		const session = await stripe.checkout.sessions.create({
			line_items: lineItems as any,
			mode: 'payment',
			success_url: `${process.env.NEXT_PUBLIC_SITE_URL}/shop?success=1`,
			cancel_url: `${process.env.NEXT_PUBLIC_SITE_URL}/shop?canceled=1`,
		});
		return NextResponse.json({ url: session.url });
	} catch (e: any) {
		return NextResponse.json({ error: e.message }, { status: 400 });
	}
}
