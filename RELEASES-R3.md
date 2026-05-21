Release 3 — Admin Dashboard
Goal: A web dashboard for school administrators to view compliance data from R2.
Scope

Admin login (Supabase Auth — email/password)
School-level view: daily/weekly compliance rate across all students
Student-level view: individual focus session history, bypass events
Key metrics: average session length, compliance %, bypass frequency
Date range filter
CSV export of any view

Tech Stack

Next.js + Tailwind (or SvelteKit — Architect decides)
Hosted on Vercel or Supabase Edge
Reads only from Supabase (no direct device communication)

Acceptance Criteria

 Admin can log in and see their school's data only
 Dashboard loads with < 2s for up to 500 students
 All charts accurate against raw Supabase data
 CSV export includes all displayed fields