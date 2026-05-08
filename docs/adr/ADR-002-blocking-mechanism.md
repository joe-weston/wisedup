# ADR-002: Blocking Mechanism for Focus Mode

## Status
Accepted — 2026-05-08

## Context

R1 must prevent the student from using any other app while Focus Mode is active, while still allowing a one-tap voluntary exit. The student installs the app themselves — no MDM, no school IT provisioning, no work profile. Android offers three plausible primitives:

1. **`AccessibilityService`** — observes system events (window state changes, foreground app changes) and can react by launching activities or dispatching gestures. User grants permission once in Settings.
2. **`DevicePolicyManager` (DPM) / Device Owner** — true device admin with `setLockTaskPackages`, app whitelisting, etc. Requires either device-owner provisioning (factory reset + QR code or NFC) or a work profile (managed profile via Android Enterprise).
3. **Lock Task Mode (screen pinning)** — `startLockTask()` pins one app to the foreground. Without device-owner status, the system shows a "To unpin, hold Back + Recents" prompt that any student can bypass in two seconds.

We also need persistence across screen-off, notification shade pulls, app switcher invocation, and reboot (covered by ADR-003).

R1 is **voluntary**. Defeating the lock is not a security concern in the threat model — accountability is. The mechanism must make the lock *clear and present* without making the install path require IT.

## Decision

**Use `AccessibilityService` for foreground-app detection and lock-screen relaunch, paired with a foreground `Service` for process persistence.**

Specifically:
- `FocusAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` events. When focus is active and the foreground package is anything other than our app, it calls `startActivity` on `FocusActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT`.
- `FocusForegroundService` runs with `foregroundServiceType="specialUse"` (API 34+) holding a persistent notification. This keeps the process alive and gives the OS a clear reason for the long-running service.
- `FocusActivity` itself is `singleTask`, `excludeFromRecents=true`, `showWhenLocked=true`, `turnScreenOn=true`, and overrides `onBackPressed`/`onUserLeaveHint` to no-op (with the voluntary "Exit Focus" button as the only egress).

User grants `BIND_ACCESSIBILITY_SERVICE` and `POST_NOTIFICATIONS` once during onboarding. Onboarding deep-links to `Settings.ACTION_ACCESSIBILITY_SETTINGS` with copy explaining what the app does and why.

## Consequences

### Positive
- Works on every Android 10+ device without IT involvement, factory reset, or work profile setup.
- Permission grant is a one-time UX cost; subsequent launches are silent.
- Voluntary exit is trivial: tap "Exit Focus" → repository sets `is_active=false` → service stops listening → activity finishes.
- Survives notification shade and app switcher because every backgrounding event triggers a relaunch within ~50–200 ms.
- Honors the "voluntary, accountability-first" mission — it's a brick, not a jail. Determined bypass is possible (Settings → Disable accessibility service); R2 will log that as a `bypass_attempted` event.

### Negative
- Accessibility-service grant has a scary system warning ("can observe your actions"). We need clear onboarding copy explaining the use. This is a real conversion-rate risk.
- Brief flicker (~100–300 ms) is visible when relaunching the lock activity over another app. Acceptable.
- A user who knows Android can disable the accessibility service from Settings. R1 cannot prevent this and shouldn't try. R2 logs the bypass.
- Google Play review: apps using `AccessibilityService` for non-accessibility purposes are policy-flagged. Mitigation: clearly disclose use in the app, in the listing, and in the Play Console accessibility-use form. Risk: rejection or restricted distribution. PM must flag this for the engineer.

### Neutral
- `targetSdk=34` requires `foregroundServiceType="specialUse"` with a `<property>` declaring use case. Document this in the manifest.

## Alternatives Considered

### `DevicePolicyManager` + Device Owner
- **Pros:** Real lock-task mode, app whitelisting, can prevent uninstall, can hide other apps. Industrially sound.
- **Cons:** Requires device-owner provisioning — factory reset and QR/NFC enrollment, *or* `dpm set-device-owner` via ADB. No student is doing that on their personal phone. School IT involvement would be required, which the mission explicitly forbids.
- **Verdict:** Rejected. Violates the "no IT involvement" acceptance criterion.

### `DevicePolicyManager` + Work Profile (Android Enterprise)
- **Pros:** Doesn't need factory reset; user creates a managed profile.
- **Cons:** Still requires an EMM/MDM as the profile owner. Same "needs IT" problem. Also splits the device into work/personal, which is wrong UX for "I'm at school now."
- **Verdict:** Rejected.

### `startLockTask()` without device owner (screen pinning)
- **Pros:** Built-in, no special permission.
- **Cons:** System shows the "hold Back + Recents to unpin" hint. Trivially defeated in 2 seconds. No relaunch on bypass.
- **Verdict:** Rejected. Doesn't meet the "all other apps inaccessible" criterion.

### `SYSTEM_ALERT_WINDOW` overlay
- **Pros:** Draws over everything.
- **Cons:** Intentionally restricted on modern Android (banner notifications, Play policy heat). Doesn't block input to apps below; they keep running.
- **Verdict:** Rejected as primary mechanism. May be used as a fallback for the "boot completed but accessibility not yet rebound" race, scoped to a few seconds.

### `UsageStatsManager` polling
- **Pros:** Detects foreground app without accessibility service.
- **Cons:** Polling, battery cost, and `PACKAGE_USAGE_STATS` is itself a special permission (Settings deep-link, same UX cost as accessibility) with no event-driven hook.
- **Verdict:** Rejected.

## Related Decisions
- ADR-001: Native Kotlin (required for direct `AccessibilityService` subclassing).
- ADR-003: Reboot persistence (BootReceiver re-arms this mechanism).

## References
- AccessibilityService: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Foreground service types (Android 14): https://developer.android.com/about/versions/14/changes/fgs-types-required
- Play policy on accessibility use: https://support.google.com/googleplay/android-developer/answer/10964491
