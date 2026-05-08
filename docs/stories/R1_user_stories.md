# R1 User Stories — Android Brick

**Release:** R1 (Core Focus Mode, Android-only)
**Status:** Approved for build — 2026-05-08
**Source docs:** MISSION.md (R1), R1_architecture.md, ADR-001..004, R1_workflows.md
**Audience:** the Engineer agent (Kotlin / Jetpack Compose). One pass, no further clarification needed.

## Conventions

- Package: `com.wisedup.focus`
- minSdk 29, targetSdk 34, Kotlin 1.9.x, AGP 8.5.x, Compose BOM 2024.06.00
- Persistence: Jetpack DataStore Preferences (file `wisedup_state.preferences_pb`)
- Color tokens: `green` = inactive, `red`/`amber` = active
- Tap-count metric: counted from cold launch on the **second** open onward (onboarding is one-time)
- All manual tests are emulator-friendly unless flagged "physical device only"

---

## US-R1-01 — Project bootstrap (Gradle + manifest skeleton)

**Narrative:**
As an engineer, I want a buildable Android project skeleton so that subsequent stories have a working foundation.

**Acceptance criteria:**
- Given a clean checkout, when I run `./gradlew :app:assembleDebug`, then the build succeeds and produces an APK.
- Given the manifest, when I open it, then it declares: `BIND_ACCESSIBILITY_SERVICE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Given the manifest, when inspected, then `<application>` references `WisedUpApplication`, `MainActivity` is the launcher, and the package layout matches `R1_architecture.md` §"Module / Package Structure".
- Given the manifest, when inspected, then the app declares `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` and both XML files exclude `wisedup_state.preferences_pb`.
- Given a release variant, when built, then R8 is enabled and the build succeeds.

**Test notes:**
- `./gradlew :app:assembleDebug :app:assembleRelease` both succeed.
- `aapt dump permissions app-debug.apk` lists all six permissions above.
- Install on emulator (Pixel 6 API 34) — app icon appears in launcher.

**Dependencies:** none.

---

## US-R1-02 — DataStore + repositories

**Narrative:**
As the app, I want a single persistent source of truth for student profile and focus state so that every component (UI, services, receiver) reads the same flag.

**Acceptance criteria:**
- Given a fresh install, when DataStore is read, then defaults are: `student.id` absent, `student.display_name` absent, `student.created_at_ms` absent, `focus.is_active` = `false`, `focus.started_at_ms` = `0L`.
- Given `StudentProfileRepository.completeOnboarding("Alex M.")`, when called once, then `student.id` is a UUIDv4, `student.display_name` = "Alex M.", `student.created_at_ms` = `System.currentTimeMillis()` at call time (±1 s), and `profile` flow emits non-null.
- Given `student.display_name` length 0 after `trim()`, when `completeOnboarding` is called, then it throws `IllegalArgumentException` and writes nothing.
- Given `student.display_name` length 65, when `completeOnboarding` is called, then it throws `IllegalArgumentException` and writes nothing.
- Given `FocusStateRepository.activate()`, when called, then `focus.is_active` = `true` and `focus.started_at_ms` = `System.currentTimeMillis()` at call time (±1 s).
- Given `FocusStateRepository.deactivate()`, when called, then `focus.is_active` = `false` and `focus.started_at_ms` = `0L`.
- Given `FocusStateRepository.snapshot()`, when called from a non-coroutine context (e.g. `BroadcastReceiver.goAsync()`), then it returns a `FocusState` within 10 s without crashing.
- Given a corrupt `student.id` (non-UUID string), when read, then it is regenerated and treated as fresh-install state.

**Test notes:**
- Unit tests use DataStore in-memory test rule + Turbine.
- `FocusStateRepositoryTest`: cover activate/deactivate/snapshot, concurrent writes from two coroutines.
- `StudentProfileRepositoryTest`: cover empty, whitespace-only, exactly-64-char, 65-char names.

**Dependencies:** US-R1-01.

---

## US-R1-03 — First-launch onboarding (display name)

**Narrative:**
As a student, I want to enter my name once on first launch so that the app knows who I am without involving school IT.

**Acceptance criteria:**
- Given a fresh install, when I open the app, then `OnboardingActivity` is shown (not `MainActivity`'s home).
- Given the onboarding screen, when rendered, then I see (a) a title "Welcome to WisedUp", (b) a single text field labeled "Your name", (c) a primary button "Continue" that is **disabled** while the trimmed input is empty.
- Given a non-empty trimmed name (1..64 chars), when I tap "Continue", then `StudentProfileRepository.completeOnboarding(name)` is called, the activity finishes, and `MainActivity` opens.
- Given a name longer than 64 chars, when typed, then the field stops accepting characters at 64 (or shows a counter).
- Given onboarding is already complete (profile flow non-null), when I cold-launch the app, then `OnboardingActivity` is **skipped** and `MainActivity` opens directly.
- Given the user backgrounds the onboarding screen mid-entry, when they return, then any text already typed is preserved (Compose `rememberSaveable`).
- Per locked PM decision #1: **no accessibility-permission prompt is shown during onboarding.** It is requested later, after the user taps Activate (US-R1-05).

**Test notes:**
- Manual: install fresh, launch — onboarding shows. Type "Alex M." — Continue enables. Tap — home appears. Force-stop, relaunch — home appears directly (onboarding skipped).
- Compose UI test: `OnboardingScreenTest` covers disabled-button, max-length, navigation.

**Dependencies:** US-R1-02.

---

## US-R1-04 — Home screen (Focus Off, green, Activate CTA)

**Narrative:**
As a student, I want a clear home screen showing my focus is off and a single big button to turn it on so that I can activate in ≤ 2 taps.

**Acceptance criteria:**
- Given onboarding is complete and `focus.is_active` = `false`, when `MainActivity` renders, then I see (a) a green status badge with text "Focus Off", (b) my display name, (c) a primary button "Activate Focus" that is the visually dominant element, (d) no other interactive controls except an info/about affordance.
- Given the home screen, when measured against MISSION.md "≤ 2 taps to activate", then the path from cold launch to active focus is exactly **one tap** ("Activate Focus") on the second-and-subsequent launches (onboarding is a one-time cost).
- Given `focus.is_active` flips to `true` while the home screen is visible (e.g. via reboot resume), when the flow emits, then the screen reflects the active state immediately (`collectAsStateWithLifecycle`).
- Given the home screen, when the user pulls the notification shade or switches apps and returns, then the screen state matches the persisted flag (no stale UI).
- Given the device is in dark mode, when the home screen renders, then the green badge remains legible (contrast ratio ≥ 4.5:1 against background).

**Test notes:**
- Manual: launch on second open — verify exactly one prominent button. Tap count from app-icon press to focus-active = 2 (icon + Activate).
- Compose UI test: `HomeScreenTest` covers green-state rendering, button presence, dark-mode contrast.

**Dependencies:** US-R1-03.

---

## US-R1-05 — Activate flow with accessibility-permission request

**Narrative:**
As a student, I want tapping Activate to walk me through granting the accessibility permission the first time so that the app can keep me focused without confusing me up front.

**Acceptance criteria:**
- Given `FocusAccessibilityService` is **not enabled** in system settings, when I tap "Activate Focus", then the app shows a rationale dialog: title "One-time setup", body explaining "WisedUp uses Android Accessibility to keep you on the focus screen. Tap Open Settings, find WisedUp, and turn it on. Then come back.", primary button "Open Settings", secondary button "Cancel".
- Given the rationale dialog, when I tap "Open Settings", then the app launches `Settings.ACTION_ACCESSIBILITY_SETTINGS` via `Intent`.
- Given I return to the app **without** enabling the service, when `MainActivity.onResume` fires, then the home screen remains in `Focus Off` state, no error toast, no auto-retry.
- Given `FocusAccessibilityService` is enabled and `POST_NOTIFICATIONS` is granted (on Android 13+), when I tap "Activate Focus" again, then `FocusController.activate()` runs: `focus.is_active` = `true`, `started_at_ms` = `now`, `FocusForegroundService` is started via `ContextCompat.startForegroundService`, and `FocusActivity` is launched.
- Given Android 13+ and `POST_NOTIFICATIONS` not yet granted, when I tap "Activate Focus" with the accessibility service enabled, then the runtime permission prompt is shown first; on grant, activation proceeds; on deny, activation is aborted with a toast "Notification permission required to show focus status."
- Per locked PM decision #1, this is the only place where accessibility permission is solicited. Onboarding does not solicit it.

**Test notes:**
- Manual (emulator): fresh install → onboard → tap Activate → rationale dialog appears → Open Settings → toggle WisedUp on → press back → tap Activate again → notification permission prompt → Allow → focus screen appears.
- Manual: deny notification permission — verify toast and that app stays on home.

**Dependencies:** US-R1-02, US-R1-04, US-R1-09.

---

## US-R1-06 — FocusActivity full-screen lock UI

**Narrative:**
As a student, I want a clear red/amber "Focus Active" screen with my name and a voluntary Exit button so that I always know the app is doing its job and how to turn it off.

**Acceptance criteria:**
- Given `FocusActivity` is launched, when it renders, then the surface fills the screen with the `red` or `amber` color token, shows the text "Focus Active", the student's display name, an elapsed-time label updating at least every 1 s (format `MM:SS` for first hour, then `HH:MM:SS`), and a single button labeled "Exit Focus".
- Given the manifest declaration, when inspected, then `FocusActivity` uses `android:launchMode="singleTask"`, `android:excludeFromRecents="true"`, `android:showWhenLocked="true"`, `android:turnScreenOn="true"`, and a fullscreen theme (status/nav bar hidden or color-matched).
- Given the user presses the system Back button while on `FocusActivity`, when handled, then nothing happens (override to no-op).
- Given the user invokes `onUserLeaveHint` (Home press), when handled, then nothing additional happens in the activity (the AccessibilityService relaunches it — see US-R1-08).
- Given `focus.is_active` becomes `false` (via voluntary exit or external state change), when the flow emits, then `FocusActivity.finish()` is called.
- Given the device is rotated, when configuration changes, then the activity does not flicker into a non-focused state and the elapsed timer continues uninterrupted.

**Test notes:**
- Compose UI test: render with `isActive=true`, verify color token and Exit button. Render with `started_at_ms = now - 90_000`, verify label shows `01:30`.
- Manual: with focus active, press Back — nothing happens; press Home — screen flickers and reasserts within ~300 ms.

**Dependencies:** US-R1-02, US-R1-07 (foreground service must be running for activity to make sense).

---

## US-R1-07 — Foreground Service with persistent notification

**Narrative:**
As the system, I want a foreground service holding a high-importance notification while focus is active so that the OS keeps the process alive and the student always sees the status.

**Acceptance criteria:**
- Given `FocusForegroundService.onStartCommand`, when called, then it calls `startForeground(NOTIF_ID, notification)` within 5 s and returns `START_STICKY`.
- Given the manifest, when inspected, then `<service android:name=".focus.FocusForegroundService" android:foregroundServiceType="specialUse" android:exported="false">` is declared, and contains a `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="focus_lock_persistence" />` with a justification string in `strings.xml`.
- Given the notification, when posted, then channel ID = `focus_mode` (created in `WisedUpApplication.onCreate` with `IMPORTANCE_HIGH`), title = "Focus Mode active", text = "Tap to return to focus screen", `setOngoing(true)`, content intent = `PendingIntent` to `FocusActivity` with `FLAG_IMMUTABLE`.
- Given the notification, when the user attempts to swipe-dismiss, then it cannot be dismissed (because `setOngoing(true)` and the service is foreground).
- Given `FocusController.deactivate()`, when called, then `FocusForegroundService.stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()` are called and the notification is removed.
- Given the OS kills the service under memory pressure, when restarted by `START_STICKY`, then it re-reads `focus.is_active`; if `true`, it re-posts the notification; if `false`, it calls `stopSelf()` immediately.

**Test notes:**
- Manual: activate focus → pull notification shade → see "Focus Mode active" notification, ongoing, non-dismissible. Tap it → returns to FocusActivity.
- Manual: with focus active, run `adb shell am kill com.wisedup.focus` — service should restart (system-driven), notification reappears within ~5 s.

**Dependencies:** US-R1-02.

---

## US-R1-08 — AccessibilityService relaunches FocusActivity

**Narrative:**
As the focus enforcer, I want to detect any other app coming to the foreground and immediately relaunch the lock screen so that the student cannot use other apps while focus is active.

**Acceptance criteria:**
- Given `res/xml/accessibility_service_config.xml`, when inspected, then it declares `accessibilityEventTypes="typeWindowStateChanged"`, `accessibilityFeedbackType="feedbackGeneric"`, `notificationTimeout="50"`, `canRetrieveWindowContent="false"`, and a `description` resource with honest copy explaining the focus-enforcement use case.
- Given the manifest, when inspected, then `<service>` for `FocusAccessibilityService` declares `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, the intent filter `android.accessibilityservice.AccessibilityService`, and the meta-data linking to the XML config.
- Given `focus.is_active` = `true` and the foreground package becomes anything other than `com.wisedup.focus`, when `onAccessibilityEvent` fires with `TYPE_WINDOW_STATE_CHANGED`, then the service starts `FocusActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT` within 200 ms (measured on Pixel 6 emulator).
- Given `focus.is_active` = `false`, when `onAccessibilityEvent` fires, then the service does **nothing** (no relaunch, no log spam).
- Given the foreground package equals `com.wisedup.focus`, when the event fires, then no relaunch happens (avoid relaunch loops on our own activity transitions).
- Given a system UI surface (e.g. `com.android.systemui` notification shade), when it appears, then it is treated like any other foreground change — `FocusActivity` is reasserted.

**Test notes:**
- Manual (emulator): activate focus → from FocusActivity press Home → launcher appears for ~100–300 ms → FocusActivity is back. Try opening Calculator from launcher → flickers and returns. Pull notification shade → FocusActivity reasserts on dismiss.
- Per locked PM decision #4: after screen lock + unlock, FocusActivity is the foreground when the user reaches the home/lock surface — verified manually.

**Dependencies:** US-R1-02, US-R1-06, US-R1-07.

---

## US-R1-09 — Notification channel + POST_NOTIFICATIONS handling

**Narrative:**
As the app, I want a properly-configured notification channel and runtime permission flow so that the foreground-service notification displays reliably on Android 13+.

**Acceptance criteria:**
- Given `WisedUpApplication.onCreate`, when called, then it creates the `focus_mode` `NotificationChannel` with `IMPORTANCE_HIGH`, name "Focus Mode", description "Shown while WisedUp Focus is active so you always know your status." (idempotent on subsequent launches).
- Given Android 13+ (API 33+) and `POST_NOTIFICATIONS` not granted, when activation is attempted, then the runtime permission prompt is requested via `ActivityResultContracts.RequestPermission()` (see US-R1-05).
- Given Android 12 or below, when the app starts, then no runtime permission prompt is shown (channel suffices).
- Given the user revokes notification permission while focus is active, when the foreground service tries to post, then the service does not crash; behavior degrades gracefully (notification not visible, but blocking still works via accessibility service).

**Test notes:**
- Manual (API 34 emulator): clean install → activate → permission prompt → Allow. Then Settings → WisedUp → Notifications → Off → return to focus screen → app does not crash.
- Manual (API 29 emulator): activate → no permission prompt → notification appears.

**Dependencies:** US-R1-01, US-R1-02.

---

## US-R1-10 — Voluntary exit with confirm dialog

**Narrative:**
As a student, I want exiting focus mode to require a confirmation tap so that I don't accidentally leave the focus screen.

**Acceptance criteria:**
- Given `FocusActivity` is visible, when I tap "Exit Focus", then a Material `AlertDialog` is shown with title "Exit Focus Mode?", body "Your session will be logged in a future release.", confirm button "Exit", dismiss button "Stay focused".
- Per locked PM decision #2: the dialog copy is exactly the strings above. Do not soften or dramatize.
- Given the dialog is visible, when I tap "Stay focused" or tap outside, then the dialog dismisses and `FocusActivity` remains active.
- Given the dialog is visible, when I tap "Exit", then `FocusController.deactivate()` runs: `focus.is_active` = `false`, `focus.started_at_ms` = `0L`, `FocusForegroundService.stopSelf()`, `FocusActivity.finish()`, and the user lands on `MainActivity` home in `Focus Off` (green) state.
- Given the dialog is visible, when the AccessibilityService observes another foreground change, then it does **not** dismiss the dialog (the dialog is hosted in our own activity, so no relaunch loop).
- Given the user backgrounds the device while the dialog is open, when they return, then either the dialog or the focus screen is shown — not the home screen (state remains active until "Exit" tapped).

**Test notes:**
- Manual: with focus active, tap Exit → dialog appears → tap "Stay focused" → dialog gone, still focused. Tap Exit → tap "Exit" → home screen, green state.
- UI test: confirm dialog renders, Exit invokes controller.deactivate, Cancel does nothing.

**Dependencies:** US-R1-06, US-R1-07.

---

## US-R1-11 — BootReceiver re-enters Active after reboot

**Narrative:**
As a student, I want focus mode to come back automatically after a reboot so that rebooting isn't a free bypass.

**Acceptance criteria:**
- Given the manifest, when inspected, then `BootReceiver` is declared with intent filters for `android.intent.action.BOOT_COMPLETED` and `android.intent.action.LOCKED_BOOT_COMPLETED`, `android:exported="true"`, `android:directBootAware="false"` (we wait for user-unlocked storage for DataStore).
- Given `focus.is_active` = `true` and the device reboots, when `BOOT_COMPLETED` fires, then within 10 s `BootReceiver` (a) reads the snapshot via `goAsync()`, (b) calls `ContextCompat.startForegroundService(...)`, and the persistent notification appears.
- Given `focus.is_active` = `false` and the device reboots, when `BOOT_COMPLETED` fires, then `BootReceiver` no-ops; no service is started; no notification appears.
- Per locked PM decision #3: after reboot, focus mode resumes silently — no toast, no re-onboarding, no banner. The persistent foreground-service notification is the only signal.
- Given the AccessibilityService rebinds 5–30 s after boot, when it does, then it resumes its relaunch loop and the next foreground-app change re-asserts FocusActivity (this gap is documented behavior, not a defect — see ADR-003).

**Test notes:**
- Manual (physical device or `adb reboot` on emulator):
  1. Activate focus.
  2. Reboot device.
  3. Within 10 s of unlock, persistent notification "Focus Mode active" is visible in the shade.
  4. Tap notification → FocusActivity opens.
  5. Open another app from the launcher → FocusActivity reasserts (may take up to 30 s for accessibility rebind on first attempt).
- Repeat with focus inactive — no notification appears post-reboot.

**Dependencies:** US-R1-02, US-R1-07, US-R1-08.

---

## US-R1-12 — Survival of background pressure (shade, recents, lock/unlock)

**Narrative:**
As a student, I want the focus screen to reassert itself through every common Android escape hatch so that I genuinely cannot use other apps while focus is active.

**Acceptance criteria:**
- Given focus is active, when I pull down the notification shade and dismiss it, then FocusActivity is the topmost window within 500 ms.
- Given focus is active, when I open the app switcher (Recents), then either (a) FocusActivity is excluded from recents (per `excludeFromRecents=true`) or (b) tapping any other entry causes FocusActivity to reassert within 500 ms.
- Given focus is active, when I press the power button to lock the screen and then unlock it, then per locked PM decision #4 FocusActivity is the foreground activity once unlock completes (the AccessibilityService relaunches it).
- Given focus is active, when I press Home, then the launcher is briefly visible (~100–300 ms acceptable flicker) and FocusActivity reasserts.
- Given focus is active, when the device runs out of memory and the OS kills our process, when the system relaunches the sticky service, then within 5 s the notification reappears and the next foreground-app change brings FocusActivity back.
- Given focus is active, when the user toggles airplane mode, then nothing breaks (no crash; R1 has no network dependency).

**Test notes:**
- Manual scenario matrix (run on Pixel 6 emulator API 34):
  | Action | Expected |
  |---|---|
  | Pull shade, swipe to dismiss | FocusActivity returns ≤ 500 ms |
  | Tap Home | Launcher flickers, FocusActivity returns |
  | Open Recents, tap Calculator | FocusActivity reasserts |
  | Power off + on screen | FocusActivity is foreground after unlock |
  | `adb shell am kill com.wisedup.focus` | Notification returns ≤ 5 s; activity returns on next foreground change |
  | Airplane mode toggle | No crash, no visible change |
- Document any device-specific deviation (Samsung, Xiaomi) as known issues, not blockers, per ADR-003.

**Dependencies:** US-R1-06, US-R1-07, US-R1-08.

---

## US-R1-13 — Visual state correctness (green ↔ red/amber)

**Narrative:**
As a student, I want the app's color and copy to match its actual state so that I never confuse "off" for "on".

**Acceptance criteria:**
- Given `focus.is_active` = `false` anywhere in the app, when any screen renders, then primary status uses the `green` token and the word "Off" appears in the badge.
- Given `focus.is_active` = `true`, when any screen renders, then primary status uses the `red` or `amber` token and the word "Active" appears in the badge.
- Given the state changes (via activate, deactivate, reboot resume, or external write), when the change is persisted, then all currently-visible UI updates within one frame after the flow emits (no manual refresh, no stale text).
- Given dark mode, when each screen renders, then green/red/amber tokens meet WCAG AA contrast (≥ 4.5:1) against their backgrounds.
- Given the persistent notification, when posted, then its title and small icon visually correspond to the active state (e.g. icon tint matching `red`/`amber`).

**Test notes:**
- UI test: snapshot tests for `HomeScreen` in both states and `FocusScreen` rendering.
- Manual: activate → confirm red/amber. Exit → confirm green. Force-stop and reopen → state persists and color is correct on first frame.

**Dependencies:** US-R1-04, US-R1-06.

---

## US-R1-14 — Definition of Done & MISSION.md acceptance sign-off

**Narrative:**
As QA, I want a single checklist mapping MISSION.md acceptance criteria to verifiable tests so that I can sign off R1 line-by-line.

**Acceptance criteria (cross-cutting — covers all 5 MISSION.md ACs):**

| # | MISSION.md AC | Verification | Covered by |
|---|---|---|---|
| 1 | Student can open app and activate Focus Mode in ≤ 2 taps | Cold launch on second open: tap 1 = app icon (launch), tap 2 = "Activate Focus" button. Confirmation dialog is **not** required for activation (only for exit). Onboarding is one-time and excluded from this metric. | US-R1-04, US-R1-05 |
| 2 | All other apps are inaccessible during Focus Mode | Run the manual matrix in US-R1-12 + free-form attempt: open Settings, Chrome, Calculator, Phone, Camera, launcher widgets — each is replaced by FocusActivity within 500 ms. | US-R1-06, US-R1-08, US-R1-12 |
| 3 | Focus Mode survives device reboot | Activate focus → reboot via `adb reboot` and via physical power button → on unlock, persistent notification visible within 10 s; FocusActivity reasserts on next foreground change (≤ 30 s). | US-R1-11, US-R1-12 |
| 4 | Student can voluntarily exit at any time | From FocusActivity, tap "Exit Focus" → confirm dialog → "Exit" → home screen, green state, notification gone. Verified at: 0 s, 30 s, 5 min, post-reboot, with shade open, with no network, in dark mode. | US-R1-10 |
| 5 | App does not require school IT involvement to install | Install path: download from Play Store (or sideload APK) → launch → onboarding → grant accessibility from Settings → grant notification permission → activate. No MDM, no work profile, no factory reset, no QR enrollment, no ADB. The accessibility-permission grant is the only system-settings deep-link. | US-R1-03, US-R1-05, ADR-002 |

**Definition of Done for R1:**
- All 14 stories' ACs pass.
- Manual test matrix from US-R1-12 passes on Pixel 6 emulator API 34.
- Reboot test from US-R1-11 passes on at least one physical device.
- `./gradlew :app:assembleRelease :app:test` is green.
- `aapt dump permissions` shows only the six permissions in US-R1-01 (no `INTERNET`, no `SYSTEM_ALERT_WINDOW`, no `BIND_DEVICE_ADMIN`).
- Auto Backup exclusion verified: install app, complete onboarding, trigger `adb shell bmgr backupnow`, restore on a different emulator → app shows onboarding screen (not the previous student's name).
- Play Console accessibility-use disclosure draft exists (out-of-app deliverable, but flagged here per ADR-002 risk).

**Test notes:**
- This story has no code. It is the QA gate.
- QA produces a sign-off doc per AC, attached to the R1 PR.

**Dependencies:** US-R1-01 through US-R1-13.

---

## Story → MISSION AC Map (quick reference)

| MISSION AC | Primary stories | Secondary stories |
|---|---|---|
| ≤ 2 taps to activate | US-R1-04, US-R1-05 | US-R1-03 (onboarding excluded from metric) |
| All other apps inaccessible | US-R1-08, US-R1-12 | US-R1-06, US-R1-07 |
| Survives reboot | US-R1-11 | US-R1-07, US-R1-12 |
| Voluntary exit | US-R1-10 | US-R1-06 |
| No IT involvement to install | US-R1-03, US-R1-05 | US-R1-01 (manifest only) |

## Out of scope for R1 (do not implement)

- Any network call, including telemetry, crash reporting (add Crashlytics in R2 if needed).
- School code or school-issued ID (deferred to R2 per locked PM decision #5).
- Bypass-attempt logging (R2).
- Any UI other than Onboarding, Home, FocusActivity, and the Exit confirm dialog.
- Battery-optimization-exemption prompt UX is **optional**; if implemented, it must not block activation.
- DevicePolicyManager, screen pinning fallback, SYSTEM_ALERT_WINDOW overlay (all rejected in ADR-002).
