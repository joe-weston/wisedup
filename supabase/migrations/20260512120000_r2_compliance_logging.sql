-- R2: schools, students, focus_sessions, bypass_events + SECURITY DEFINER RPCs.
-- See docs/adr/ADR-005-r2-compliance-logging.md

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

CREATE TABLE public.schools (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    code text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT schools_code_nonempty CHECK (length(trim(code)) >= 1)
);

CREATE UNIQUE INDEX schools_code_lower_uidx ON public.schools (lower(trim(code)));

CREATE TABLE public.students (
    id uuid PRIMARY KEY,
    school_id uuid NOT NULL REFERENCES public.schools (id) ON DELETE CASCADE,
    display_name text NOT NULL,
    device_id text NOT NULL,
    sync_token_hash text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT students_display_name_len CHECK (
        char_length(display_name) BETWEEN 1 AND 64
    )
);

CREATE INDEX students_school_id_idx ON public.students (school_id);

CREATE TABLE public.focus_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES public.students (id) ON DELETE CASCADE,
    school_id uuid NOT NULL REFERENCES public.schools (id) ON DELETE CASCADE,
    client_session_id uuid NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    duration_seconds integer,
    CONSTRAINT focus_sessions_duration_nonneg CHECK (
        duration_seconds IS NULL OR duration_seconds >= 0
    ),
    CONSTRAINT focus_sessions_unique_client UNIQUE (student_id, client_session_id)
);

CREATE INDEX focus_sessions_student_started_idx
    ON public.focus_sessions (student_id, started_at DESC);

CREATE INDEX focus_sessions_school_started_idx
    ON public.focus_sessions (school_id, started_at DESC);

CREATE TABLE public.bypass_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES public.students (id) ON DELETE CASCADE,
    session_id uuid REFERENCES public.focus_sessions (id) ON DELETE SET NULL,
    event_type text NOT NULL,
    event_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT bypass_events_type_chk CHECK (
        event_type = ANY (ARRAY['accessibility_disabled'::text, 'other'::text])
    )
);

CREATE INDEX bypass_events_student_at_idx
    ON public.bypass_events (student_id, event_at DESC);

-- ---------------------------------------------------------------------------
-- RLS: enabled, no policies for anon/authenticated on tables (default deny).
-- service_role bypasses RLS for future admin tooling (R3).
-- ---------------------------------------------------------------------------

ALTER TABLE public.schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.schools FORCE ROW LEVEL SECURITY;

ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students FORCE ROW LEVEL SECURITY;

ALTER TABLE public.focus_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.focus_sessions FORCE ROW LEVEL SECURITY;

ALTER TABLE public.bypass_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bypass_events FORCE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.schools FROM PUBLIC;
REVOKE ALL ON TABLE public.students FROM PUBLIC;
REVOKE ALL ON TABLE public.focus_sessions FROM PUBLIC;
REVOKE ALL ON TABLE public.bypass_events FROM PUBLIC;

GRANT ALL ON TABLE public.schools TO postgres;
GRANT ALL ON TABLE public.students TO postgres;
GRANT ALL ON TABLE public.focus_sessions TO postgres;
GRANT ALL ON TABLE public.bypass_events TO postgres;

-- R3 / server jobs: service_role bypasses RLS and can read all rows.
GRANT ALL ON TABLE public.schools TO service_role;
GRANT ALL ON TABLE public.students TO service_role;
GRANT ALL ON TABLE public.focus_sessions TO service_role;
GRANT ALL ON TABLE public.bypass_events TO service_role;

-- students/focus_sessions/bypass_events/schools: no direct grants to anon/authenticated.

-- ---------------------------------------------------------------------------
-- RPC helpers
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public._student_sync_ok(
    p_student_id uuid,
    p_sync_token text
) RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.students s
        WHERE s.id = p_student_id
          AND s.sync_token_hash IS NOT NULL
          AND crypt(p_sync_token, s.sync_token_hash) = s.sync_token_hash
    );
$$;

REVOKE ALL ON FUNCTION public._student_sync_ok(uuid, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public._student_sync_ok(uuid, text) TO postgres;

CREATE OR REPLACE FUNCTION public.register_student(
    school_code text,
    student_id uuid,
    display_name text,
    device_id text
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_school public.schools%ROWTYPE;
    v_token text;
    v_hash text;
    v_name text;
BEGIN
    IF school_code IS NULL OR length(trim(school_code)) < 1 THEN
        RAISE EXCEPTION 'invalid_school_code';
    END IF;

    v_name := trim(display_name);
    IF length(v_name) < 1 OR length(v_name) > 64 THEN
        RAISE EXCEPTION 'invalid_display_name';
    END IF;

    SELECT *
    INTO v_school
    FROM public.schools s
    WHERE lower(trim(s.code)) = lower(trim(school_code))
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'invalid_school_code';
    END IF;

    v_token := encode(gen_random_bytes(32), 'base64');
    v_hash := crypt(v_token, gen_salt('bf'));

    INSERT INTO public.students (id, school_id, display_name, device_id, sync_token_hash)
    VALUES (student_id, v_school.id, v_name, device_id, v_hash)
    ON CONFLICT (id) DO UPDATE SET
        school_id = EXCLUDED.school_id,
        display_name = EXCLUDED.display_name,
        device_id = EXCLUDED.device_id,
        sync_token_hash = EXCLUDED.sync_token_hash;

    RETURN jsonb_build_object(
        'school_id', v_school.id,
        'sync_token', v_token
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.submit_focus_session_start(
    student_id uuid,
    sync_token text,
    school_id uuid,
    client_session_id uuid,
    started_at timestamptz
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_ok boolean;
BEGIN
    SELECT public._student_sync_ok(student_id, sync_token) INTO v_ok;
    IF NOT v_ok THEN
        RAISE EXCEPTION 'auth_failed';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.students s
        WHERE s.id = student_id AND s.school_id = school_id
    ) THEN
        RAISE EXCEPTION 'auth_failed';
    END IF;

    INSERT INTO public.focus_sessions (
        student_id,
        school_id,
        client_session_id,
        started_at,
        ended_at,
        duration_seconds
    )
    VALUES (
        student_id,
        school_id,
        client_session_id,
        started_at,
        NULL,
        NULL
    )
    ON CONFLICT (student_id, client_session_id) DO NOTHING;

    RETURN jsonb_build_object('ok', true);
END;
$$;

CREATE OR REPLACE FUNCTION public.submit_focus_session_end(
    student_id uuid,
    sync_token text,
    client_session_id uuid,
    ended_at timestamptz,
    duration_seconds integer
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_ok boolean;
    v_count integer;
BEGIN
    SELECT public._student_sync_ok(student_id, sync_token) INTO v_ok;
    IF NOT v_ok THEN
        RAISE EXCEPTION 'auth_failed';
    END IF;

    UPDATE public.focus_sessions fs
    SET
        ended_at = submit_focus_session_end.ended_at,
        duration_seconds = submit_focus_session_end.duration_seconds
    WHERE fs.student_id = submit_focus_session_end.student_id
      AND fs.client_session_id = submit_focus_session_end.client_session_id;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count = 0 THEN
        RETURN jsonb_build_object('ok', false, 'reason', 'no_session');
    END IF;

    RETURN jsonb_build_object('ok', true);
END;
$$;

CREATE OR REPLACE FUNCTION public.submit_bypass_event(
    student_id uuid,
    sync_token text,
    client_session_id uuid,
    event_type text,
    event_at timestamptz
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_ok boolean;
    v_session_id uuid;
BEGIN
    SELECT public._student_sync_ok(student_id, sync_token) INTO v_ok;
    IF NOT v_ok THEN
        RAISE EXCEPTION 'auth_failed';
    END IF;

    IF event_type IS NULL OR event_type NOT IN ('accessibility_disabled', 'other') THEN
        RAISE EXCEPTION 'invalid_event_type';
    END IF;

    SELECT fs.id
    INTO v_session_id
    FROM public.focus_sessions fs
    WHERE fs.student_id = submit_bypass_event.student_id
      AND fs.client_session_id = submit_bypass_event.client_session_id
    LIMIT 1;

    INSERT INTO public.bypass_events (student_id, session_id, event_type, event_at)
    VALUES (student_id, v_session_id, event_type, event_at);

    RETURN jsonb_build_object('ok', true);
END;
$$;

REVOKE ALL ON FUNCTION public.register_student(text, uuid, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.submit_focus_session_start(uuid, text, uuid, uuid, timestamptz) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.submit_focus_session_end(uuid, text, uuid, timestamptz, integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.submit_bypass_event(uuid, text, uuid, text, timestamptz) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION public.register_student(text, uuid, text, text) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_focus_session_start(uuid, text, uuid, uuid, timestamptz) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_focus_session_end(uuid, text, uuid, timestamptz, integer) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_bypass_event(uuid, text, uuid, text, timestamptz) TO anon, authenticated;

-- Dev seed (Architect/QA: replace codes in non-prod environments as needed)
INSERT INTO public.schools (name, code)
SELECT 'Demo High School', 'DEMO-001'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.schools s
    WHERE lower(trim(s.code)) = lower(trim('DEMO-001'))
);
