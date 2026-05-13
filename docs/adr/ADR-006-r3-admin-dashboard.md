# ADR-006: R3 Admin Dashboard (Auth, RLS, Compliance Metrics)

## Status

Accepted — 2026-05-13

## Context

Release 3 ([MISSION.md](../../MISSION.md) §Release 3) adds a **read-only web dashboard** for school staff. R2 ([ADR-005](./ADR-005-r2-compliance-logging.md)) left **no `SELECT` grants** to `anon`/`authenticated` on compliance tables; only `service_role` and postgres can read. Admins must authenticate (**Supabase Auth, email/password**) and see **only their school’s** rows.

## Decision

### 1. Compliance % (school-level, daily / weekly)

**Definition (MVP):** For a given calendar **day** or **ISO week** (Monday start) and a date filter range:

- **Enrolled students** = count of rows in `public.students` where `students.school_id` = the admin’s school (all students, not only those active in range).
- **Compliant in bucket** = distinct `students.id` who have **at least one** `focus_sessions` row where:
  - `focus_sessions.school_id` matches the school,
  - `started_at` falls inside that **day** or **week** bucket (UTC `date_trunc`),
  - `ended_at IS NOT NULL` (session completed),
  - `duration_seconds IS NOT NULL` and `duration_seconds >= 60` (filters zero-length or aborted noise).

**Compliance %** for that bucket = `100 * (compliant_student_count / NULLIF(enrolled_student_count, 0))`, rounded for display. If enrolled count is 0, show null / "—".

**Bypass frequency:** count of `bypass_events` per bucket (same `date_trunc` on `event_at`), optional normalization later; MVP uses raw counts in range.

### 2. Admin identity

- Table **`public.school_admins`** (`user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE`, `school_id uuid NOT NULL REFERENCES public.schools(id) ON DELETE CASCADE`, `created_at timestamptz DEFAULT now()`).
- **One school per admin** in MVP (`user_id` primary key).
- **Provisioning:** operators insert `school_admins` after creating the user in Supabase Auth (Dashboard or API). No self-service school pick in v1.

### 3. Read path: RLS + `authenticated`

- **`GRANT SELECT`** on `schools`, `students`, `focus_sessions`, `bypass_events`, `school_admins` to **`authenticated`**.
- **RLS policies** restrict reads so `auth.uid()` resolves to a `school_admins` row and all visible rows belong to that `school_id`.
- **`service_role`** remains for server jobs; **never** embed it in the Next.js client bundle.

### 4. Dashboard performance

- School overview uses **aggregated queries** (date buckets, single pass over `focus_sessions` / `bypass_events` in range) plus **paginated** student list — no N+1 per student for the overview.

## Consequences

- Admins cannot read other schools’ data without a matching `school_admins` row.
- Compliance % is **session-based**, not "minutes of focus required" (that would need policy/config tables).

## Related

- ADR-005: R2 logging and student RPC auth.
- [supabase/migrations](../../supabase/migrations): `*_r3_admin_dashboard.sql`.
