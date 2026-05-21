# ADR-003: Persistence and Reboot Behavior

## Status
Accepted — 2026-05-08

## Context

R1 acceptance criterion: "Focus Mode survives device reboot." This is the hardest requirement in R1 because Android aggressively kills processes, withholds startup signals from new apps, and increasingly restricts background activity launches.

Survival surfaces we must handle:
1. **Screen lock / unlock** — process stays alive; activity may pause. Trivial.
2. **Notification shade pull-down** — focus activity is briefly not the topmost window. Handled by ADR-002 (accessibility service relaunches).
3. **App switcher (Recents)** — same as above; relaunch on `TYPE_WINDOW_STATE_CHANGED`.
4. **Cold reboot** — process dies completely; no activity, no service, no listeners. **This is what this ADR solves.**
5. **Low-memory kill** — system kills the foreground service. Mitigated by `START_STICKY` + `foregroundServiceType="specialUse"`.
6. **Force-stop from Settings** — user explicitly stops the app. Cannot recover until next launch by user. Acceptable; this is a deliberate bypass and will be logged in R2.
7. **Airplane mode toggled** — irrelevant in R1 (no network); confirm no crash path.

Relevant Android constraints:
- `BOOT_COMPLETED` is delivered to apps that have been launched at least once since install (since Android 3.1). Onboarding launch satisfies this.
- Background activity launches are restricted (Android 10+). We need a path that the system accepts: foreground service notification → user taps notification → activity launches; or the accessibility service callback launches the activity once it rebinds.
- `AccessibilityService` is automatically restarted by the system after reboot if it was enabled, but rebind can lag 5–30 seconds.

## Decision

**Persist focus state in DataStore Preferences. On `BOOT_COMPLETED`, a `BootReceiver` reads the flag and starts `FocusForegroundService`, which posts a high-priority "Focus Mode is active — tap to resume" notification. The accessibility service rebinds independently and resumes lock-screen relaunches as soon as it does.**

Sequence on reboot when `is_active=true`:

1. Device boots.
2. System delivers `android.intent.action.BOOT_COMPLETED` to `BootReceiver`.
3. `BootReceiver` reads `is_active` from DataStore (synchronous-blocking read inside a `goAsync()` block, ≤ 10 s budget).
4. If active, calls `ContextCompat.startForegroundService(ctx, Intent(ctx, FocusForegroundService::class.java))`.
5. `FocusForegroundService.onStartCommand` calls `startForeground(NOTIF_ID, buildNotification())` within 5 s. Notification is `IMPORTANCE_HIGH`, ongoing, non-dismissible (`setOngoing(true)`), with content intent → `FocusActivity`.
6. As soon as `FocusAccessibilityService` rebinds (system-driven), it resumes the relaunch loop from ADR-002.
7. Bridging gap (boot → accessibility rebind): the persistent notification is the user's tappable resume path. If the student opens any app in this window, the lock activity launches when accessibility binds; if they tap the notification, it launches immediately.

Repository contract:
```kotlin
interface FocusStateRepository {
    val isActive: Flow<Boolean>
    val startedAtMs: Flow<Long?>
    suspend fun activate()                  // sets is_active=true, started_at_ms=now
    suspend fun deactivate()                // sets is_active=false, clears started_at_ms
    suspend fun snapshot(): FocusState      // synchronous-style read for receivers
}
```

## Consequences

### Positive
- Single source of truth (`DataStore`). Activity, service, accessibility service, and boot receiver all read the same flag.
- Survives reboot, low-memory kill (`START_STICKY` + foreground notification), and screen-off.
- No `WorkManager` needed for R1 — periodic work would add complexity without solving the cold-start problem.
- DataStore is async by default; the synchronous read inside `goAsync()` is the only blocking call and is bounded.

### Negative
- 5–30 s window between boot and accessibility-service rebind where a student could open another app. Mitigated by the persistent notification and by the fact that the lock activity will reassert itself the moment the accessibility service rebinds. Documented behavior.
- Force-stop from Settings cannot be recovered without a user action. Out of scope for R1; R2 logs the absence.
- `FOREGROUND_SERVICE_SPECIAL_USE` requires manifest `<property>` justification on Android 14+. Document the focus-enforcement use case.

### Neutral
- Notification channel `focus_mode` must be created on first launch with `IMPORTANCE_HIGH`. User can downgrade it; we accept that.

## Edge Cases

| Case | Behavior |
|------|----------|
| Reboot while `is_active=false` | `BootReceiver` reads flag, no-ops. |
| Airplane mode | No effect. R1 has no network. Verify no `ConnectivityManager` calls in hot path. |
| Battery saver / Doze | Foreground service is exempt from Doze. Accessibility service is exempt while bound. Safe. |
| OEM aggressive killers (Xiaomi, Huawei, OnePlus) | Foreground notification + accessibility binding usually survive. Document a "battery optimization exemption" prompt during onboarding (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Best-effort; not all OEMs honor it. |
| Storage full, DataStore write fails | Repository returns failure; UI surfaces error toast; flag stays at last known value. |
| User clears app data | `is_active` resets to default (`false`). Acceptable — same as fresh install. |
| Crash inside `FocusActivity` | Foreground service stays alive; user sees notification; tapping it relaunches activity. |
| Concurrent writes from activity and service | DataStore serializes writes per-file; safe. |

## Alternatives Considered

### `WorkManager` periodic check
- **Pros:** Survives reboot via `WorkManager`'s own boot-completed receiver.
- **Cons:** Minimum 15 min interval, doze-deferred, not designed for "always on" UX. Wrong tool.
- **Verdict:** Rejected.

### `JobScheduler` with `setPersisted(true)`
- **Pros:** Persists across reboot.
- **Cons:** Same delay/throttling problems as WorkManager (it's the underlying primitive).
- **Verdict:** Rejected.

### Use `SharedPreferences` instead of DataStore
- **Pros:** Synchronous read API, simpler in `BootReceiver`.
- **Cons:** Deprecated for new code; main-thread I/O hazards; will need to migrate for R2 anyway.
- **Verdict:** Rejected. Use DataStore Preferences with bounded blocking read inside `goAsync()`.

### Self-signed device admin (`DeviceAdminReceiver`, not Device Owner)
- **Pros:** Some lock primitives.
- **Cons:** Deprecated for most uses since Android 9; doesn't grant lock-task mode without device-owner.
- **Verdict:** Rejected.

## Related Decisions
- ADR-002: Blocking mechanism (this ADR re-arms it on boot).
- ADR-004: Data model (defines the persisted fields).

## References
- Boot completed broadcast: https://developer.android.com/develop/background-work/background-tasks/scheduling/alarms#boot
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Foreground services: https://developer.android.com/develop/background-work/services/foreground-services
