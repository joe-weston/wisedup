# ADR-005: R2 Compliance Logging (Supabase + Offline Outbox)

## Status

Accepted — 2026-05-12

## Context

Release 2 ([MISSION.md](../../MISSION.md) §Release 2) adds **network-backed behavioral logging**: every focus start/end and bypass attempt is stored so schools can have transparent, data-driven accountability conversations—not MDM enforcement.

Constraints from the mission and R1 decisions:

- **Voluntary / no student auth UI in R2** — no email/password for students; association to a school is via a **school code** typed once at registration.
- **ADR-004** already defines `student.id` as UUIDv4 in DataStore, stable across releases. That identifier must map cleanly to Supabase `students.id`.
- **ADR-002** documents that disabling the accessibility service is an intentional bypass path; R2 must record **`bypass_attempted`** with a typed `event_type`.
- **Offline-first** — events must queue locally and sync when connectivity returns.
- **No service-role keys in the app** — the Android client must never embed `service_role`.
- **RLS** must prevent students from reading other students’ rows; the student client has no legitimate read path for peer data in R2.

## Decision

### 1. Primary keys and identity

- **`students.id` is the UUID from DataStore `student.id` (ADR-004).** The Android app uses this UUID as the primary key on first successful `register_student` upsert. No separate server-generated student key.
- **`device_id`** is `Settings.Secure.ANDROID_ID` (best-effort stable per device+user profile). Used for support/dedup analytics only; not a secret.

### 2. Single source of truth for focus timing

- **`focus_sessions` is the canonical log** for focus on/off. There is **no separate `events` table** in R2; R3 dashboards query `focus_sessions` and `bypass_events` only.
- **`client_session_id`** (UUID generated on the device when a session starts) correlates start/end RPCs and links `bypass_events` to the active session row server-side.

### 3. `bypass_events.event_type` (closed set)

| Value | Meaning |
|-------|---------|
| `accessibility_disabled` | Accessibility service unbound while focus was still marked active (Settings disable or package force-stop). |
| `other` | Reserved for future detectors; do not emit from R2 Android except tests. |

Foreground “escape” relaunches are **not** logged as bypasses—they are normal ADR-002 behavior.

### 4. Authentication model (no JWT custom claims)

The **Supabase publishable** API key is shipped in the client (same privilege model as the legacy “anon” key; normal Supabase pattern) but **direct table access from the `anon` Postgres role is revoked**. All student writes go through **`SECURITY DEFINER` RPCs** that:

1. Validate a **per-student `sync_token`** returned once from `register_student`, stored as **`crypt(..., gen_salt('bf'))`** in `students.sync_token_hash`.
2. Re-verify `student_id` + `sync_token` + (where applicable) `school_id` on every subsequent RPC.

**`register_student`** (callable by `anon`):

- Input: `school_code`, `student_id`, `display_name`, `device_id`
- Validates `schools.code` (case-insensitive trim match).
- Upserts `students` and **rotates** `sync_token_hash`, returning a fresh **`sync_token`** once in the JSON payload.

The Android app persists `school_id` + `sync_token` in DataStore next to existing student keys (see §Consequences).

### 5. Local storage split (ADR-004 unchanged)

- **DataStore** remains the source of truth for `student.*`, `focus.*`, and new **`student.school_id`**, **`student.sync_token`** keys.
- **Room** (`WizedUp.db`) holds only the **outbox** of pending RPC payloads (`pending_sync` table). This matches ADR-004’s migration-path note.

### 6. Event capture wiring

- A **`ComplianceCoordinator`** (application scope) observes `FocusStateRepository.state` and enqueues outbox rows on **edges** `inactive→active` (focus_started payload) and `active→inactive` (focus_ended), with an **initialization guard** so cold-start / reboot reads of `isActive=true` do not emit duplicate starts.
- **`FocusStateRepository.activate`** writes `focus.client_session_id` (new UUID) when entering active state; **`deactivate`** clears it.
- **`FocusAccessibilityService.onUnbind`**: if focus is still active, enqueue **`bypass_attempted`** with `accessibility_disabled`.

### 7. Sync transport

- **WorkManager** (`UniqueWork` + backoff) drains the Room outbox by invoking PostgREST **`/rpc/<function>`** via **Ktor** ([`SupabaseRestClient`](../../apps/android/src/main/java/com/wizedup/focus/data/remote/SupabaseRestClient.kt)) using `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_PUBLISHABLE_KEY` (from `local.properties`). Registration itself runs **inline** in the registration ViewModel (not outboxed) so the user gets immediate error feedback.

## Consequences

### Positive

- **No peer reads** from the student app: RLS + revoked table grants mean even a curious student cannot `select` classmates’ sessions via REST.
- **No service role in APK**; compromise of the **publishable** key alone does not grant bulk read/write to session history without per-student `sync_token`.
- **Clear mapping** from ADR-004 `student.id` to `students.id`.

### Negative

- **Lost `sync_token`** (clear data / new install) requires calling `register_student` again; old server rows remain keyed by the same `student_id` if the UUID is restored from backup—ADR-004 already recommends excluding DataStore from backup; we align `student.sync_token` with that posture.
- **RPC surface area** must be reviewed whenever new write paths are added (same as any locked-down BFF).

### Neutral

- **R3 admin** will use Supabase Auth + policies or `service_role` on a trusted server; out of scope for R2 beyond reserving `service_role` for server-side jobs.

## Related Decisions

- ADR-002: Blocking + bypass semantics.
- ADR-004: Local student identity + Room introduction in R2.

## References

- Supabase RLS: https://supabase.com/docs/guides/auth/row-level-security
- PostgREST RPC: https://postgrest.org/en/stable/references/api/stored_procedures.html
- `pgcrypto` `crypt`: https://www.postgresql.org/docs/current/pgcrypto.html
