# R1 Architecture — Android Brick

## Status
Accepted — 2026-05-08. Implements ADR-001 through ADR-004.

## One-Paragraph Summary

WizedUp R1 is a native Kotlin Android app, minSdk 29 / targetSdk 34, built with Jetpack Compose. The student onboards once (display name + auto-generated UUID), grants `BIND_ACCESSIBILITY_SERVICE` and `POST_NOTIFICATIONS`, and from then on can toggle Focus Mode in two taps. When active, a foreground service holds a persistent notification, an accessibility service watches `TYPE_WINDOW_STATE_CHANGED` events and relaunches `FocusActivity` whenever any other app reaches the foreground, and DataStore persists the flag. On reboot, a `BootReceiver` reads the flag and starts the foreground service, which keeps state visible until the accessibility service rebinds. A clearly labeled "Exit Focus" button is the only voluntary egress. No network. No logging.

## Module / Package Structure

Single-module Gradle project. Reasonable package layout:

```
app/
  src/main/
    kotlin/com/wizedup/focus/
      WizedUpApplication.kt
      ui/
        MainActivity.kt
        OnboardingActivity.kt
        FocusActivity.kt
        theme/                       // Compose theme, color tokens (red/amber/green)
        components/                  // FocusToggleCard, ExitButton, StateBadge
      focus/
        FocusAccessibilityService.kt
        FocusForegroundService.kt
        FocusController.kt           // pure logic: activate(), deactivate()
      boot/
        BootReceiver.kt
      data/
        FocusStateRepository.kt
        StudentProfileRepository.kt
        DataStoreModule.kt           // DataStore<Preferences> singleton
        Keys.kt                      // typed key definitions
      util/
        Notifications.kt             // channel creation, builder
        Intents.kt                   // settings deep-links
    res/
      xml/
        accessibility_service_config.xml
        backup_rules.xml             // exclude DataStore from auto-backup
        data_extraction_rules.xml
    AndroidManifest.xml
  src/test/                          // unit tests for repositories, controller
  src/androidTest/                   // instrumentation tests for service lifecycle
```

If multi-module is preferred later (R2+), split: `:core-data`, `:core-ui`, `:feature-onboarding`, `:feature-focus`, `:app`. Not needed for R1.

## Key Components

| Component | Type | Responsibility |
|---|---|---|
| `WizedUpApplication` | `Application` | Initialize DataStore, create notification channel, install global crash handler. |
| `MainActivity` | `ComponentActivity` | Entry point. Routes to Onboarding if not onboarded, else to a small home screen showing current state and a `Toggle Focus` button. |
| `OnboardingActivity` | `ComponentActivity` | One screen: enter display name → confirm. Then deep-link prompts for accessibility permission and notification permission. Writes `student.*` keys. |
| `FocusActivity` | `ComponentActivity` | The lock screen. Single composable, full-screen, red/amber background when locked. Shows display name, elapsed time since `started_at_ms`, and a single `Exit Focus` button. `singleTask`, `excludeFromRecents`, `showWhenLocked`, `turnScreenOn`. Overrides back/recents to no-op. |
| `FocusAccessibilityService` | `AccessibilityService` | On `TYPE_WINDOW_STATE_CHANGED`, if `focus.is_active` and foreground package ≠ our own, launch `FocusActivity` with `NEW_TASK \| CLEAR_TOP \| REORDER_TO_FRONT`. |
| `FocusForegroundService` | `Service` (`foregroundServiceType="specialUse"`) | Holds the persistent notification. `START_STICKY`. Lifecycle is started by `FocusController.activate()` and by `BootReceiver` after reboot. Stopped by `FocusController.deactivate()`. |
| `FocusController` | Plain class (DI-able) | Pure logic. `activate()` writes flag, starts service, launches activity. `deactivate()` writes flag, stops service, finishes activity. Single place to test focus transitions. |
| `BootReceiver` | `BroadcastReceiver` | Listens for `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED`. Reads `focus.is_active` in `goAsync()` block. If true, calls `ContextCompat.startForegroundService(...)`. |
| `FocusStateRepository` | Repo | DataStore-backed `Flow<FocusState>` + suspend mutators. Used by activity, service, controller, receiver. |
| `StudentProfileRepository` | Repo | DataStore-backed `Flow<StudentProfile?>` + onboarding completion. |

## State Machine

```
[ Inactive (green) ]
        │
        │ user taps "Activate Focus"
        ▼
[ Activating ]
        │ writes is_active=true, started_at_ms=now
        │ startForegroundService(FocusForegroundService)
        │ startActivity(FocusActivity)
        ▼
[ Active (red/amber) ]
        │
        ├─ user taps "Exit Focus"  ─────► writes is_active=false → service.stop() → activity.finish() → [Inactive]
        ├─ system event: another app foreground → AccessibilityService relaunches FocusActivity → stays [Active]
        ├─ device reboot → BootReceiver → starts service → notification visible → AccessibilityService rebinds → [Active]
        └─ user force-stops app → process dies; on next launch by user, state honors persisted flag
```

## Permissions

Manifest declarations and rationale:

| Permission | Purpose | Notes |
|---|---|---|
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Required to declare `FocusAccessibilityService`. | System-only permission on the service tag. User grants via Settings deep-link in onboarding. |
| `android.permission.FOREGROUND_SERVICE` | Required for any `startForeground(...)` call. | Normal install-time permission on Android 9+. |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Required because our service type is `specialUse` on Android 14+. | Add `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="focus_lock_persistence" />` with a justification string. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Allow `BootReceiver` to receive `BOOT_COMPLETED`. | Normal install-time permission. |
| `android.permission.POST_NOTIFICATIONS` | Show the persistent focus notification on Android 13+. | Runtime permission; request during onboarding. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional onboarding prompt to whitelist for battery optimization. | Helps OEMs that aggressively kill background services. Not required to function. |

**Not used:**
- `SYSTEM_ALERT_WINDOW` — not needed; activity-relaunch covers all observed cases. If field testing reveals gaps, revisit.
- `BIND_DEVICE_ADMIN` / DPM — explicitly rejected in ADR-002.
- `PACKAGE_USAGE_STATS` — unnecessary; accessibility events are enough.
- Internet, network state — R1 has no network.

## Build Configuration

| Setting | Value |
|---|---|
| Kotlin | 1.9.24 |
| AGP (Android Gradle Plugin) | 8.5.2 |
| Gradle | 8.7 |
| compileSdk | 34 |
| targetSdk | 34 |
| minSdk | 29 |
| Java toolchain | 17 |
| Compose BOM | 2024.06.00 |
| Compose Compiler | matched to Kotlin 1.9.24 (or use Kotlin 2.0 + Compose Compiler Plugin if upgraded) |
| AndroidX Core KTX | latest stable |
| AndroidX Lifecycle | 2.8.x |
| AndroidX DataStore Preferences | 1.1.x |
| Coroutines | 1.8.x |
| Hilt (DI) | optional; if used, 2.51.x |
| JUnit / Truth / Turbine | for unit tests |

`buildFeatures { compose = true }`. R8 enabled in release. `multidex` not needed at this size.

## UI Choice — Compose

Use Jetpack Compose for all UI surfaces. Justification:

- Three screens total (Onboarding, Home, Focus). Compose halves the boilerplate vs. XML + ViewBinding.
- The `FocusActivity` is essentially one state-driven composable: `Surface(color = if (isActive) Color.Red else Color.Green)` plus a button and an elapsed-time label collected from `Flow`.
- Live state collection via `collectAsStateWithLifecycle()` makes the repository → UI wiring trivial.
- Compose Multiplatform leaves a future door open for shared UI with iOS (R4) without committing now.

The accessibility service config remains XML (`res/xml/accessibility_service_config.xml`) — that's a system requirement.

## Notification Channel

Single channel:
- ID: `focus_mode`
- Name: "Focus Mode"
- Importance: `IMPORTANCE_HIGH`
- Description: "Shown while WizedUp Focus is active so you always know your status."
- Created in `WizedUpApplication.onCreate()`.

Notification itself: ongoing, non-dismissible while service is foreground, content title "Focus Mode active", content text e.g. "Tap to return to focus screen", content intent → `FocusActivity`.

## Testing Strategy

| Layer | Tooling | Coverage Target |
|---|---|---|
| Repositories | JUnit + Turbine + DataStore in-memory | 90%+ |
| `FocusController` | JUnit + fake repository, fake service launcher | 100% of state transitions |
| `BootReceiver` | Robolectric or instrumentation | flag-true and flag-false paths |
| `FocusActivity` | Compose UI tests | render states, exit button calls controller |
| `FocusAccessibilityService` | Instrumentation, manual device matrix | event handling, relaunch latency |
| End-to-end | Manual on Pixel 6 (AOSP-clean), Samsung A-series, Xiaomi (OEM-aggressive) | full ACs from MISSION |

## Acceptance-Criteria Traceability

| Mission AC | Covered by |
|---|---|
| Activate in ≤ 2 taps | `MainActivity` home → `Toggle Focus` button → `FocusController.activate()`. One tap from home. (Onboarding is one-time.) |
| All other apps inaccessible | `FocusAccessibilityService` relaunch loop (ADR-002). |
| Survives reboot | `BootReceiver` + foreground service + accessibility rebind (ADR-003). |
| Voluntary exit | "Exit Focus" button in `FocusActivity` → `FocusController.deactivate()`. |
| No school IT involvement | Standard Play Store install + user-granted accessibility permission (ADR-002). |

## Open Risks for the Engineer

1. **Play Store accessibility-use review.** Disclose the focus-enforcement use case explicitly in the listing and the Play Console accessibility-use form. Risk of rejection or restricted distribution. Have a side-load APK ready for pilot schools as fallback.
2. **OEM battery optimizers** (Xiaomi/MIUI, Huawei/EMUI, OnePlus/OxygenOS, Samsung/OneUI's "Deep Sleep"). Foreground service + battery-optimization-exemption prompt is best-effort; field-test on at least one device per OEM.
3. **Boot → accessibility rebind gap (5–30 s).** Notification is the bridge; document this clearly to QA so it isn't filed as a defect.
4. **Auto Backup of DataStore** could clone `student.id` to a restored device. Configure `data_extraction_rules.xml` to exclude `wizedup_state.preferences_pb`.
5. **`targetSdk=34` foreground-service-type rules.** Ensure `<property>` declaration is correct or the service start will throw at runtime.
6. **R4 iOS rewrite is non-trivial.** This architecture deliberately does not pretend to be cross-platform. R4 will need its own ADR.

## Related Documents
- ADR-001: Language and framework
- ADR-002: Blocking mechanism
- ADR-003: Persistence and reboot
- ADR-004: Local data model
- MISSION.md: Release 1 — Android Brick
