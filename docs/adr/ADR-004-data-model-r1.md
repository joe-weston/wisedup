# ADR-004: Local Data Model for R1

## Status
Accepted — 2026-05-08

## Context

R1 has no backend, no logging, no network. The data model is intentionally tiny: a student profile (entered once at onboarding) and a focus state (toggled by the user). The decision is what storage primitive to use and how to shape the schema so that R2 (Supabase + offline event queue, likely Room) does not require a painful migration.

R2 will introduce:
- `focus_sessions` event log (id, started_at, ended_at, duration_seconds).
- `bypass_events` log.
- A school registration step (school code → `school_id`).
- An offline sync queue.

R1's schema must not commit to anything that conflicts with the R2 schema in `MISSION.md`. In particular: the `student_id` we generate locally in R1 must be a stable identifier that R2 can reuse when registering with Supabase.

## Decision

**Use Jetpack DataStore Preferences (not Proto, not Room) for R1. Store two namespaces of keys: `student.*` and `focus.*`. Generate `student.id` as a UUIDv4 at first launch and never change it.**

### Schema

DataStore file: `wisedup_state.preferences_pb`

| Key | Type | Default | Notes |
|---|---|---|---|
| `student.id` | String (UUIDv4) | generated on first launch | Stable across R1 → R2. Becomes the Supabase `students.id` value (or maps to it via `device_id`). |
| `student.display_name` | String | "" | Free text, entered at onboarding. Max 64 chars (validated at boundary). |
| `student.created_at_ms` | Long | `System.currentTimeMillis()` at first launch | Epoch millis UTC. |
| `focus.is_active` | Boolean | `false` | Single source of truth for ADR-002 / ADR-003. |
| `focus.started_at_ms` | Long? (sentinel `0L = null`) | `0L` | Set when activated, cleared on deactivate. |

### Repository Layer

Two repositories, each backed by the same DataStore instance:

```kotlin
class StudentProfileRepository(private val ds: DataStore<Preferences>) {
    val profile: Flow<StudentProfile?>           // null until onboarding complete
    suspend fun completeOnboarding(displayName: String): StudentProfile
    suspend fun isOnboarded(): Boolean
}

class FocusStateRepository(private val ds: DataStore<Preferences>) {
    val state: Flow<FocusState>                  // never null; default = inactive
    suspend fun activate()
    suspend fun deactivate()
    suspend fun snapshot(): FocusState           // synchronous-style for BootReceiver
}

data class StudentProfile(val id: String, val displayName: String, val createdAtMs: Long)
data class FocusState(val isActive: Boolean, val startedAtMs: Long?)
```

### Validation Rules

- `display_name`: trim; require length 1..64; reject only if empty after trim. No PII stripping in R1 (offline only).
- `student.id`: validate UUID format on read; if corrupt (manual file edit), regenerate and treat as fresh install.
- `focus.started_at_ms`: must be ≤ now + 1 s skew; otherwise reset to now.

## Consequences

### Positive
- Five keys total. Auditable in one screen.
- DataStore Preferences is async-safe, type-safe (via key helpers), and survives process death.
- No schema migration needed for R1 because there is effectively no schema — just keys.
- `student.id` as UUIDv4 means R2 can register the existing student into Supabase without re-keying. The Supabase `students.id` can either accept the same UUID or store the local UUID as `device_id`. Decision deferred to ADR-005 (R2).
- Adding R2's event log is additive: introduce Room with `focus_sessions` and `bypass_events` tables, leave the DataStore prefs untouched. No data migration; no destructive change.

### Negative
- DataStore has no query language. Fine for R1 (5 keys), would be wrong for R2's event queue — which is why R2 will add Room.
- If we later want richer profile fields (school code, grade, etc.), they go in R2's Room schema, leaving these prefs as the canonical "device-local identity" — slight conceptual split between two stores. Acceptable; documented in R2 ADR.

### Neutral
- File location: `applicationContext.filesDir/datastore/wisedup_state.preferences_pb`. Backed up by Android Auto Backup unless we exclude it. Recommendation: exclude via `<data-extraction-rules>` so a restored device starts fresh; otherwise `student.id` could be cloned across devices. PM should flag this for the engineer.

## Alternatives Considered

### Room database in R1
- **Pros:** Same primitive as R2; no swap later.
- **Cons:** Overweight for two records and a boolean. Schema ceremony, migrations, DAOs — none of which earn their cost when there's nothing to query.
- **Verdict:** Rejected for R1. Adopt in R2.

### `SharedPreferences`
- **Pros:** Trivial API.
- **Cons:** Main-thread I/O hazards, deprecated for new code, no `Flow` integration.
- **Verdict:** Rejected.

### Proto DataStore
- **Pros:** Type-safe schema with `.proto`.
- **Cons:** Proto toolchain, codegen, schema-version management — overkill for five keys.
- **Verdict:** Rejected.

### Encrypted storage (Tink / Jetpack Security)
- **Pros:** Encrypts at rest.
- **Cons:** No secrets in R1 (display name and a UUID are not secrets). Adds complexity. Reconsider in R2 if we cache auth tokens.
- **Verdict:** Rejected for R1.

## Migration Path to R2

R2 will:
1. Add Room database `wisedup.db` v1 with `focus_sessions`, `bypass_events`, `pending_sync` tables.
2. Add a `SchoolRegistrationRepository` that stores `school_id` and `school_code` (likely in DataStore alongside `student.*`).
3. Sync existing `student.id` to Supabase `students.id` on first network connection after R2 install.
4. Continue reading `focus.is_active` from DataStore — no change.

No destructive migration. No data loss. The DataStore file is preserved by app updates.

## Related Decisions
- ADR-003: Persistence (uses these keys).
- ADR-002: Blocking (reads `focus.is_active`).

## References
- DataStore Preferences guide: https://developer.android.com/topic/libraries/architecture/datastore
- Auto Backup configuration: https://developer.android.com/guide/topics/data/autobackup
