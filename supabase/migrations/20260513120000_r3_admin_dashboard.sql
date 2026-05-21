-- R3: Admin dashboard — school_admins + SELECT for authenticated + RLS.
-- See docs/adr/ADR-006-r3-admin-dashboard.md

-- ---------------------------------------------------------------------------
-- Admin ↔ school (one school per user in MVP)
-- ---------------------------------------------------------------------------

CREATE TABLE public.school_admins (
    user_id uuid PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    school_id uuid NOT NULL REFERENCES public.schools (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX school_admins_school_id_idx ON public.school_admins (school_id);

ALTER TABLE public.school_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_admins FORCE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.school_admins FROM PUBLIC;
GRANT ALL ON TABLE public.school_admins TO postgres;
GRANT ALL ON TABLE public.school_admins TO service_role;

-- Authenticated: read own linkage only (no insert/update via API in MVP)
GRANT SELECT ON TABLE public.school_admins TO authenticated;

CREATE POLICY school_admins_select_own
    ON public.school_admins
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Allow dashboard reads for authenticated admins (RLS-scoped by school)
-- ---------------------------------------------------------------------------

GRANT SELECT ON TABLE public.schools TO authenticated;
GRANT SELECT ON TABLE public.students TO authenticated;
GRANT SELECT ON TABLE public.focus_sessions TO authenticated;
GRANT SELECT ON TABLE public.bypass_events TO authenticated;

CREATE POLICY schools_select_admin_school
    ON public.schools
    FOR SELECT
    TO authenticated
    USING (
        id = (
            SELECT sa.school_id
            FROM public.school_admins sa
            WHERE sa.user_id = auth.uid()
            LIMIT 1
        )
    );

CREATE POLICY students_select_admin_school
    ON public.students
    FOR SELECT
    TO authenticated
    USING (
        school_id = (
            SELECT sa.school_id
            FROM public.school_admins sa
            WHERE sa.user_id = auth.uid()
            LIMIT 1
        )
    );

CREATE POLICY focus_sessions_select_admin_school
    ON public.focus_sessions
    FOR SELECT
    TO authenticated
    USING (
        school_id = (
            SELECT sa.school_id
            FROM public.school_admins sa
            WHERE sa.user_id = auth.uid()
            LIMIT 1
        )
    );

CREATE POLICY bypass_events_select_admin_school
    ON public.bypass_events
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.students s
            INNER JOIN public.school_admins sa ON sa.school_id = s.school_id
            WHERE s.id = bypass_events.student_id
              AND sa.user_id = auth.uid()
        )
    );
