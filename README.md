# blahmonster.com

Art & software studio site for Blah Monster (Long Island, NY). Next.js App Router, deployed on Cloudflare Pages.

## Stack

- **Next.js 15** + `@cloudflare/next-on-pages`
- **Content**: D1 blog (production) with `data/blog.json` fallback; `data/portfolio.json` for archive pages
- **Admin**: `/admin` — password-protected post editor (D1 + optional R2 media)

## Local development

```bash
npm install
cp .env.example .env.local
# Set BLOG_SESSION_SECRET and BLOG_ADMIN_PASSWORD (or PBKDF2 hash)
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Cloudflare Pages

```bash
npm run pages:build    # output: .vercel/output/static
npm run pages:deploy   # manual deploy via Wrangler
```

Production deploys from `main` on GitHub (`blahmonsterllc/blahmonster-site`).

**Build settings:** `npm run pages:build` → `.vercel/output/static`

Set environment variables in Pages (see `.env.example`). Bind D1 as `DB` and R2 as `MEDIA` when using the admin CMS. Run `schema/blog.sql` on the D1 database once.

## Content

| What | Where |
|------|--------|
| Homepage copy & work list | `app/(site)/page.tsx` |
| Portfolio archive | `data/portfolio.json` |
| Blog posts (fallback / seed) | `data/blog.json` |
| TapClick privacy policy | `app/legal/tapclick-privacy/page.tsx` |
| Brand assets | `public/brand/` |

Hash an admin password: `npm run hash-admin-password -- "your-password"`

## Layout

```
app/
  (site)/          # Public marketing site
  admin/           # Blog CMS UI
  api/admin/       # Auth + posts + upload
  api/blog/media/  # R2 media proxy
  legal/           # Standalone legal pages
components/        # AdminApp, PostBody
lib/blog/          # Store, auth, D1 helpers
data/              # portfolio.json, blog.json
public/            # Static assets (favicon, brand, GIF)
schema/            # D1 SQL
scripts/           # hash-admin-password.mjs
```
