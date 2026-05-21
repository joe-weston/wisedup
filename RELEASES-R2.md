Release 2 — Compliance Logging
Goal: Every Focus Mode event is logged to a Supabase backend. The Android app from R1 gains network awareness.
Scope

Supabase project setup (schema below)
Student "registration" — simple ID/name + school code on first launch
Log events: focus_started, focus_ended, bypass_attempted
Log metadata: timestamp, student_id, school_id, device_id, duration_seconds
Offline-first: queue events locally (SQLite) and sync when connected
No auth UI for students in R2 (school code = implicit org association)

Supabase Schema (draft — Architect to validate)
schools (id, name, code, created_at)
students (id, school_id, display_name, device_id, created_at)
focus_sessions (id, student_id, started_at, ended_at, duration_seconds)
bypass_events (id, student_id, session_id, event_type, timestamp)
Acceptance Criteria

 All focus events logged within 5 seconds of occurrence (when online)
 Events queued and synced correctly after offline period
 No PII beyond student display name and school-assigned ID
 Supabase RLS policies prevent students from reading other students' data