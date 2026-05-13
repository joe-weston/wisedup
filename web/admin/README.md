# WizedUp Admin (Release 3)

Next.js dashboard for school administrators. Reads compliance data from Supabase with **RLS** (`authenticated` + `school_admins`); see [ADR-006](../../docs/adr/ADR-006-r3-admin-dashboard.md).

## Setup

```bash
cd web/admin
npm install
```

Copy `.env.example` to `.env.local` and set:

- `NEXT_PUBLIC_SUPABASE_URL` — project URL  
- `NEXT_PUBLIC_SUPABASE_ANON_KEY` — **anon** publishable key (never use `service_role` in the browser)

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Sign in with a user that has a `school_admins` row (see [docs/R3_ADMIN_PROVISIONING.md](../../docs/R3_ADMIN_PROVISIONING.md)).

## Vercel

1. New project → import this repo.  
2. **Root Directory:** `web/admin`  
3. Environment variables: same as `.env.local` (`NEXT_PUBLIC_*` only).  
4. In Supabase **Authentication → URL configuration**, add:

   - Site URL: `https://<your-vercel-domain>`  
   - Redirect URLs: `https://<your-vercel-domain>/**` and `http://localhost:3000/**` for local dev  

5. Deploy, then sign in and confirm only your school’s rows appear.

## Staging / performance (acceptance)

- Seed ~500 students and representative `focus_sessions` / `bypass_events` in a **non-prod** project.  
- Use a modest date range (e.g. 14 days) and confirm **School overview** first paint + charts stay within the **&lt; 2s** target on a typical connection (aggregates are bounded by range, not by N round-trips per student).

## Scripts

| Command        | Purpose        |
|----------------|----------------|
| `npm run dev`  | Dev server     |
| `npm run build`| Production build |
| `npm run lint` | ESLint         |
