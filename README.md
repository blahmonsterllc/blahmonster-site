# Blahmonster Creative Lab

A modern, professional website for Blahmonster creative lab built with Next.js 14 and file-based content (no CMS required!).

## Features

- **Portfolio**: Showcase design projects with images and details
- **Blog**: Publish articles by editing JSON files
- **Apps Section**: Dedicated pages for your apps (Tipwise featured)
- **Shop**: Sell stickers, buttons, prints with Stripe Checkout
- **No CMS**: Simple JSON files - no database, no monthly fees

## Tech Stack

- **Framework**: Next.js 14 (App Router, React Server Components)
- **Content**: JSON files (free, no external services)
- **Payments**: Stripe Checkout (optional)
- **Hosting**: Vercel (free tier available)
- **Styling**: CSS Variables, responsive design

## Getting Started Locally

### 1. Install Dependencies
```bash
npm install
```

### 2. Configure Environment (Optional for Stripe)

Create `.env.local`:

```bash
# Site URL
NEXT_PUBLIC_SITE_URL=http://localhost:3000

# Stripe (only needed for shop)
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...
```

### 3. Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

## Adding Content

### Portfolio Projects

Edit `data/portfolio.json`:

```json
{
  "id": "unique-id",
  "title": "Project Name",
  "slug": "project-slug",
  "description": "Brief description",
  "images": ["/portfolio/image.jpg"],
  "category": "branding",
  "year": 2024
}
```

Add images to `public/portfolio/`

### Blog Posts

Edit `data/blog.json`:

```json
{
  "id": "post-id",
  "title": "Post Title",
  "slug": "post-slug",
  "date": "2024-03-15",
  "excerpt": "Brief excerpt",
  "content": "Full post content"
}
```

### Shop Products

Edit `data/products.json`:

```json
{
  "id": "product-id",
  "title": "Product Name",
  "slug": "product-slug",
  "price": 25.00,
  "stripePriceId": "price_...",
  "description": "Product description",
  "images": ["/shop/image.jpg"],
  "category": "print"
}
```

Add product images to `public/shop/`

For Stripe integration:
1. Create products in [Stripe Dashboard](https://dashboard.stripe.com)
2. Copy the Price ID (starts with `price_`)
3. Paste into `stripePriceId` field

## Deploy to Vercel

### Quick Deploy

1. Push code to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Click **New Project** → Import your repo
4. Add environment variables (if using Stripe)
5. Click **Deploy**

### Custom Domain

1. In Vercel project → **Settings** → **Domains**
2. Add `blahmonster.com`
3. Update DNS at your registrar:
   - Point `A` record to Vercel IP
   - Or use Vercel nameservers

## Customization

- **Colors**: Edit `app/globals.css` CSS variables
- **Logo**: Replace `public/brand/blahmonster-logo.svg`
- **Layout**: Modify `app/layout.tsx`
- **Content**: Edit JSON files in `data/`

## File Structure

```
blahmonster.com/
├── app/                  # Next.js pages
│   ├── page.tsx         # Homepage
│   ├── portfolio/       # Portfolio pages
│   ├── blog/            # Blog pages
│   ├── shop/            # Shop pages
│   └── tipwise/         # Tipwise page
├── data/                # Content (edit these!)
│   ├── portfolio.json
│   ├── blog.json
│   └── products.json
├── public/              # Static assets
│   ├── brand/          # Logo
│   ├── portfolio/      # Project images
│   └── shop/           # Product images
└── README.md
```

## Why No CMS?

- **Free**: No Sanity/Contentful monthly fees
- **Simple**: Edit JSON files directly
- **Fast**: No API calls, statically generated
- **Portable**: Easy to migrate or backup
- **Version Control**: Content lives in git

## Support

- **Next.js**: [nextjs.org/docs](https://nextjs.org/docs)
- **Stripe**: [stripe.com/docs/checkout](https://stripe.com/docs/checkout)
- **Vercel**: [vercel.com/docs](https://vercel.com/docs)

---

Built with ❤️ by Blahmonster Creative Lab
