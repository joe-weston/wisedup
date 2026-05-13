# R3 — Provisioning a school admin

After applying migration `20260513120000_r3_admin_dashboard.sql` (see [ADR-006](adr/ADR-006-r3-admin-dashboard.md)):

## 1. Create the user in Supabase Auth

- Dashboard: **Authentication → Users → Add user** (email + password), or
- Invite flow if configured.

Copy the new user’s **UUID** (`auth.users.id`).

## 2. Resolve the school UUID

```sql
SELECT id, name, code FROM public.schools WHERE lower(trim(code)) = lower(trim('DEMO-001'));
```

## 3. Link user to school

Run in **SQL Editor** as a privileged role (service role / postgres):

```sql
INSERT INTO public.school_admins (user_id, school_id)
VALUES (
  '<auth-user-uuid>'::uuid,
  '<school-uuid>'::uuid
)
ON CONFLICT (user_id) DO UPDATE SET school_id = EXCLUDED.school_id;
```

## 4. Dashboard env and hosting

Provisioning, env vars, and Vercel deploy notes are in [web/admin/README.md](../web/admin/README.md).

## 5. Staging load check (optional)

Seed many students/sessions in a non-prod project and open the school overview with a date range covering that data to confirm first load stays within the R3 performance target.
