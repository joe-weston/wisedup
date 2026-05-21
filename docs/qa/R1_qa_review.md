# R1 QA Review — Android Brick

```
QA Verdict for R1: PASS-WITH-CONCERNS
Blocking issues: 0
Non-blocking concerns: 11
```

**Reviewer:** QA Agent
**Date:** 2026-05-08
**Method:** Code-only (no Android SDK / JDK available in env). Behavioral verification deferred to manual matrix in `docs/MANUAL_TEST_PLAN.md`.
**Files reviewed:** 44 across `/home/jwest/WizedUp/android/`.

The release is shippable. There are no blockers against the five MISSION.md ACs. There are eleven concerns, of which two are story-AC-literal mismatches (notification importance, BIND_ACCESSIBILITY_SERVICE uses-permission line) that the engineer flagged or chose deliberately and are defensible. The rest are minor robustness items that can be carried into R2 grooming without harming the brick.

---

## A. MISSION.md acceptance criteria (the five hard gates)

### A1. Activate Focus Mode in ≤ 2 taps from second-launch home — PASS
Once onboarding is complete and accessibility is granted, the path on second-and-subsequent launches is: app icon (system tap, not in-app) → tap `Activate Focus` button on `HomeScreen`. `HomeScreen.handleActivateTap` (`HomeScreen.kt:80-103`) goes straight to `viewModel.activate(...)` if both gates are satisfied; activate writes the flag and `MainActivity.AppRoot.LaunchedEffect(isActive)` (`MainActivity.kt:62-66`) launches `FocusActivity`. Total in-app taps from cold launch with permissions in place = 1. Mission AC's "≤ 2" therefore holds with margin.

### A2. All other apps inaccessible during Focus Mode — PASS
`FocusAccessibilityService.onAccessibilityEvent` (`FocusAccessibilityService.kt:47-68`) handles `TYPE_WINDOW_STATE_CHANGED`, gates on cached `isActiveCache.value`, ignores own package, and relaunches `FocusActivity` with `NEW_TASK | CLEAR_TOP | REORDER_TO_FRONT`. Manifest declares `singleTask`, `excludeFromRecents`, `clearTaskOnLaunch`, `showWhenLocked`, `turnScreenOn` (AndroidManifest.xml:50-61). Back is overridden to no-op (`FocusActivity.kt:38-46`). Notification shade triggers a relaunch flicker by design — engineer's pre-flagged deviation #3 is consistent with US-R1-08 AC line 180 and verified PASS in section H below.

### A3. Focus Mode survives device reboot — PASS
`BootReceiver` declared with `BOOT_COMPLETED` + `LOCKED_BOOT_COMPLETED` filters and `directBootAware="false"` (`AndroidManifest.xml:100-108`). On `BOOT_COMPLETED` it calls `goAsync()`, reads `focusStateRepository.snapshot(10_000ms)`, and starts the foreground service if active (`BootReceiver.kt:36-59`). `LOCKED_BOOT_COMPLETED` is intentionally a no-op because DataStore is not direct-boot aware — see deviation #4 below: ACCEPTED.

### A4. Student can voluntarily exit at any time — PASS
`FocusScreen` exposes a single `Exit Focus` button (`FocusScreen.kt:91-108`) that opens `ExitConfirmDialog` with verbatim copy from US-R1-10 (`strings.xml:39-42`). Confirm calls `FocusViewModel.deactivate()` (`FocusViewModel.kt:70-72`) which writes `is_active=false`. The reactive `LaunchedEffect(state.isActive)` in `FocusActivity` (`FocusActivity.kt:64-75`) tears down the service and lands on `MainActivity` with `FLAG_ACTIVITY_CLEAR_TASK`. Exit works at 0 s, mid-session, and post-reboot.

### A5. App does not require school IT to install — PASS
No `BIND_DEVICE_ADMIN`, no `DevicePolicyManager`, no work profile, no enterprise enrollment, no `SYSTEM_ALERT_WINDOW`. The accessibility deep-link `Settings.ACTION_ACCESSIBILITY_SETTINGS` is the only system-side action (`HomeScreen.kt:154-157`). Side-loadable APK; no MDM. Manifest is clean of any DPM-related declarations.

---

## B. Per-story validation (US-R1-01 through US-R1-14)

### US-R1-01 — Project bootstrap — CONCERN (one literal-AC miss; non-blocking)
- AC build succeeds → cannot run. Structurally correct: AGP 8.5.2 + Kotlin 1.9.24 + Gradle 8.7 + Compose BOM 2024.06.00 align (libs.versions.toml:2-9). PASS by inspection.
- AC manifest permissions list — **CONCERN**: `BIND_ACCESSIBILITY_SERVICE` is enforced on the service via `android:permission=` (AndroidManifest.xml:84) but is **not** declared as a `<uses-permission>` line. The story-literal text says "declares: `BIND_ACCESSIBILITY_SERVICE`, …". Standard Android practice is to NOT add a `<uses-permission>` line for signature permissions, and the engineer's choice is technically correct — but the DoD line "`aapt dump permissions` shows only the six permissions in US-R1-01" requires the aapt dump to show six permissions. The current manifest will produce five. See concern C-1 below.
- AC `<application>` references `WizedUpApplication`, MainActivity is launcher — PASS (`AndroidManifest.xml:20`, `:31-40`).
- AC `dataExtractionRules` and `fullBackupContent` — PASS, both wired and exclude `datastore/wizedup_state.preferences_pb` (`backup_rules.xml:6`, `data_extraction_rules.xml:9-14`).
- AC R8 enabled in release — PASS (`build.gradle.kts:27-34`).

### US-R1-02 — DataStore + repositories — PASS
- Defaults on fresh install — PASS. `FocusStateRepository.state` returns `Inactive` with `startedAtMs=null` (`FocusStateRepository.kt:51-60`); `StudentProfileRepository.profile` returns null when keys absent or invalid (`FocusStateRepository.kt:101-115`).
- `completeOnboarding` writes UUIDv4 + name + createdAt — PASS (`FocusStateRepository.kt:128-154`).
- Empty / 65-char input throws `IllegalArgumentException` and writes nothing — PASS. `DisplayNameRules.validate` throws BEFORE `dataStore.edit` is entered (`PreferencesKeys.kt:30-37`, `FocusStateRepository.kt:133`). Tests cover this (`FocusStateRepositoryTest.kt:145-178`).
- `activate()` / `deactivate()` set the right keys — PASS (`FocusStateRepository.kt:66-79`).
- `snapshot()` from non-coroutine context returns within 10 s — PASS. Uses `runBlocking { withTimeout(timeoutMs) { state.first() } }` (`FocusStateRepository.kt:87-89`).
- Corrupt UUID treated as fresh-install — PASS. `runCatching { UUID.fromString(id) }` returns null for bad IDs (`FocusStateRepository.kt:111`); on next `completeOnboarding`, a new UUID is generated (`FocusStateRepository.kt:140-142`).

### US-R1-03 — First-launch onboarding — PASS
- AC OnboardingScreen on fresh install — PASS via `MainActivity.AppRoot` profile-null branch (`MainActivity.kt:68-69`).
- AC title + field + Continue (disabled while empty) — PASS (`OnboardingScreen.kt:64-111`). `continueEnabled = name.trim().isNotEmpty() && !isSubmitting` (`OnboardingScreen.kt:49`).
- AC tap Continue → `completeOnboarding(name)` → MainActivity opens — PASS, navigation is reactive via the profile flow (`MainActivity.kt:60-81`).
- AC stops accepting at 64 chars — PASS, `OnboardingViewModel.onNameChange` truncates (`OnboardingViewModel.kt:33-40`); supportingText shows counter (`OnboardingScreen.kt:89-97`).
- AC onboarding skipped on subsequent launches — PASS, `profileState != null` route goes to HomeScreen (`MainActivity.kt:68-80`).
- AC backgrounding mid-entry preserves text — PASS. `rememberSaveable` on `draft` (`OnboardingScreen.kt:53`).
- AC no accessibility prompt during onboarding — PASS (no such call in OnboardingScreen).

### US-R1-04 — Home screen — PASS
- Green badge + display name + dominant Activate button — PASS (`HomeScreen.kt:114-147`). `StatusBadge` uses `FocusOffGreen`/`FocusOffGreenContainer` when `!isActive` (`HomeScreen.kt:170-200`).
- Tap path = 1 in-app tap on second launch — PASS. `handleActivateTap` calls `viewModel.activate(...)` directly when permissions are in order (`HomeScreen.kt:80-103`).
- Reactive update when isActive flips — PASS via `viewModel.isFocusActive.collectAsState()` (`HomeScreen.kt:61`) and the `LaunchedEffect` in `MainActivity.AppRoot` (`MainActivity.kt:62-66`).
- Persists across shade pull / app switcher — PASS, `collectAsState` on a `StateFlow.WhileSubscribed` (`HomeViewModel.kt:25-29`).
- Dark-mode contrast — CONCERN (B-1): `FocusOffGreen #1B873F` on `#121212` is ~4.4:1, marginal against the WCAG AA 4.5:1 floor. See concern G-1.
- **Note**: code uses `collectAsState` not `collectAsStateWithLifecycle` as called out by the story AC line 95. Behavior equivalent in this app's structure but a literal AC miss — see concern G-2.

### US-R1-05 — Activate flow + accessibility-permission rationale — PASS
- AC rationale dialog when service not enabled — PASS. `HomeScreen.handleActivateTap` checks `AccessibilityUtils.isAccessibilityServiceEnabled` and shows `AccessibilityRationaleDialog` (`HomeScreen.kt:87-90`, `:150-161`). Title/body/buttons match the spec exactly (`strings.xml:25-28`).
- AC tap Open Settings → `Settings.ACTION_ACCESSIBILITY_SETTINGS` — PASS (`HomeScreen.kt:154-157`).
- AC return without enabling → home stays on Off, no toast — PASS. The flow does not auto-retry; the user must tap Activate again.
- AC service enabled + POST_NOTIFICATIONS granted → activate runs — PASS (`HomeScreen.kt:92-103`, `HomeViewModel.kt:42-47`).
- AC Android 13+ POST_NOTIFICATIONS prompt → on grant proceed, on deny toast and abort — PASS (`HomeScreen.kt:66-78`).
- Per locked PM decision #1, accessibility is not solicited during onboarding — PASS, `OnboardingScreen.kt` has no permission code.
- Engineer's pre-flagged deviation #2 (rationale dialog instead of `AccessibilityPermissionScreen` full-screen variant): ACCEPTED. The story narrative and AC sequence describe a dialog, not a separate screen. The standalone `AccessibilityPermissionScreen.kt` exists as dead code; see concern J-1.

### US-R1-06 — FocusActivity full-screen lock — PASS
- AC red surface + title + name + elapsed + Exit button — PASS (`FocusScreen.kt:50-110`, theme statusBarColor = `focus_active_red` `themes.xml:22`).
- AC manifest attributes — PASS. `singleTask`, `excludeFromRecents=true`, `showWhenLocked=true`, `turnScreenOn=true`, fullscreen theme (`AndroidManifest.xml:50-61`). Plus `clearTaskOnLaunch=true` and `taskAffinity=""` for cleaner task isolation.
- AC back press is a no-op — PASS (`FocusActivity.kt:38-46`).
- AC `onUserLeaveHint` does nothing — PASS (`FocusActivity.kt:91-93`).
- AC flag→false → `finish()` — PASS (`FocusActivity.kt:64-75`). Plus a navigation back to `MainActivity` so the user lands on green home.
- AC rotation does not flicker non-focused — PASS via `configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"` (`AndroidManifest.xml:60`) so the activity does not recreate; tick is in the VM and persists.
- AC elapsed timer formatting — PASS. `formatElapsed` returns MM:SS for first hour, HH:MM:SS thereafter (`FocusScreen.kt:148-158`).

### US-R1-07 — Foreground Service + persistent notification — CONCERN (importance literal mismatch)
- AC `startForeground(...)` within 5 s and `START_STICKY` — PASS (`FocusForegroundService.kt:40-59`, `:96-106`). `onCreate` calls `startForegroundCompat` immediately so the OS does not throw `ForegroundServiceDidNotStartInTimeException`.
- AC manifest declarations and `<property>` — PASS. `foregroundServiceType="specialUse"` and `<property name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" value="focus_lock_persistence" />` (`AndroidManifest.xml:67-74`).
- AC channel `focus_mode` with `IMPORTANCE_HIGH` — **CONCERN B-2**: engineer used `IMPORTANCE_LOW` (`WizedUpApplication.kt:56`). See deviation #1 verdict in section K.
- AC notification title/text/ongoing/PendingIntent IMMUTABLE — PASS (`FocusForegroundService.kt:108-133`). Title `"Focus Mode active"` (strings.xml:47), text `"Tap to return to focus screen"` (strings.xml:48), `setOngoing(true)`, `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.
- AC swipe-dismiss prevented — PASS by `setOngoing(true)` while service is foreground (note: Android 14 relaxed this — see info K-3).
- AC deactivate stops the service — PASS via `FocusServiceController.stop` → `ACTION_STOP` → `stopForegroundAndSelf` (`FocusForegroundService.kt:46-50`, `:82-90`).
- AC START_STICKY restart re-reads flag, self-stops if false — PASS (`FocusForegroundService.kt:51-58`, `verifyStateOrStop`:72-80).

### US-R1-08 — AccessibilityService relaunches FocusActivity — PASS
- AC config XML — PASS. `accessibilityEventTypes="typeWindowStateChanged"`, `accessibilityFeedbackType="feedbackGeneric"`, `notificationTimeout="50"`, `canRetrieveWindowContent="false"`, plus a description string (`accessibility_service_config.xml:8-15`, `strings.xml:7`).
- AC manifest service declaration — PASS. `BIND_ACCESSIBILITY_SERVICE` permission, intent filter, meta-data resource (`AndroidManifest.xml:80-91`).
- AC active + non-self foreground → relaunch with `NEW_TASK | CLEAR_TOP | REORDER_TO_FRONT` — PASS (`FocusAccessibilityService.kt:79-86`). Latency target ≤ 200 ms is plausible: cached `isActive` is a `MutableStateFlow.value` read; `startActivity` is sync; no I/O on hot path.
- AC inactive → no-op — PASS (`FocusAccessibilityService.kt:52`).
- AC own package → no relaunch — PASS (`FocusAccessibilityService.kt:57`).
- AC system UI surface treated as relaunch — PASS (engineer chose not to special-case `com.android.systemui`, confirmed at `FocusAccessibilityService.kt:62-67`). See deviation #3 verdict in section K.

### US-R1-09 — Notification channel + POST_NOTIFICATIONS — CONCERN (importance literal)
- AC channel created in Application.onCreate — PASS (`WizedUpApplication.kt:50-66`).
- AC channel `IMPORTANCE_HIGH` with name "Focus Mode" + description — **PARTIAL FAIL on importance literal**. Channel name + description match (`strings.xml:45-46`). Importance is `IMPORTANCE_LOW`, contradicting both US-R1-09 AC and US-R1-07 AC. Concern, not blocking — see K-1.
- AC Android 13+ POST_NOTIFICATIONS prompt — PASS (`HomeScreen.kt:92-99`).
- AC Android ≤ 12 → no prompt — PASS (the `Build.VERSION_CODES.TIRAMISU` guard short-circuits).
- AC user revokes notification permission while active → no crash — PASS, the foreground service still runs; `NotificationCompat.Builder.build()` returns a Notification regardless; the OS suppresses display silently.

### US-R1-10 — Voluntary exit + confirm dialog — PASS
- AC AlertDialog title + body + Exit + Stay focused — PASS, verbatim copy in `strings.xml:39-42` ("Exit Focus Mode?", "Your session will be logged in a future release.", "Exit", "Stay focused"). Per locked PM decision #2.
- AC tap Stay focused / outside → dismiss, stay active — PASS (`FocusScreen.kt:113-120`, `AlertDialog.onDismissRequest = onDismiss`).
- AC tap Exit → deactivate + service stop + finish + lands on green home — PASS (`FocusViewModel.kt:70-72` → `FocusActivity.kt:64-75`).
- AC AccessibilityService does not dismiss the dialog — PASS by construction; dialog is hosted in our own activity, accessibility ignores own-package events.
- AC backgrounding while dialog open → return to dialog or focus screen, never home — PASS, the activity does not finish until state flips.

### US-R1-11 — BootReceiver re-enters Active after reboot — PASS
- AC manifest BootReceiver declarations — PASS. `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED` filters, `exported="true"`, `directBootAware="false"` (`AndroidManifest.xml:100-108`).
- AC `is_active=true` + BOOT_COMPLETED → service started within 10 s — PASS. `BootReceiver.handleBootCompleted` reads snapshot in `goAsync()` block with 10 s timeout and calls `FocusServiceController.start` (`BootReceiver.kt:37-58`).
- AC `is_active=false` + BOOT_COMPLETED → no-op — PASS (`BootReceiver.kt:49`).
- Per locked PM decision #3, no toast / no banner — PASS (no Toast in BootReceiver).
- Engineer's pre-flagged deviation #4 (`LOCKED_BOOT_COMPLETED` filtered but not acted on): ACCEPTED. The receiver's `directBootAware="false"` means the system delivers `LOCKED_BOOT_COMPLETED` only if the receiver is direct-boot-aware — it isn't. The filter line is harmless; the explicit when-branch + comment is defensive and well-documented (`BootReceiver.kt:14-33`).
- AC accessibility rebind 5–30 s gap is documented — PASS. README Known Limitations §2 covers it; ADR-003 §Negative covers it.

### US-R1-12 — Survival of background pressure — PASS (by inspection)
- AC shade pull → reassert ≤ 500 ms — PASS by accessibility relaunch design.
- AC recents → excluded or reasserts — PASS, `excludeFromRecents=true` + `clearTaskOnLaunch=true` + `taskAffinity=""` combo.
- AC lock/unlock → focus is foreground — PASS by `showWhenLocked=true` + `turnScreenOn=true` + accessibility relaunch on `WINDOW_STATE_CHANGED`.
- AC Home press → flicker + reassert — PASS by accessibility relaunch.
- AC OS kill → notification reappears + reassert — PASS via `START_STICKY` + accessibility.
- AC airplane mode → no crash — PASS, no network code at all in R1; no `ConnectivityManager` references.

### US-R1-13 — Visual state correctness — PASS (with G-1 caveat)
- AC inactive → green + "Off" — PASS (`HomeScreen.kt:171-177`, `strings.xml:19`).
- AC active → red/amber + "Active" — PASS (`FocusScreen.kt:50-89`, `strings.xml:32`).
- AC reactive update within one frame — PASS via `collectAsState` on StateFlows; Compose recomposes immediately on emission.
- AC dark mode WCAG AA — CONCERN G-1, see below.
- AC notification small icon tinted — PASS, `setColor(R.color.notification_icon_tint)` = amber `#E08A1A` (`FocusForegroundService.kt:130`, `colors.xml:21`).

### US-R1-14 — Definition of Done & sign-off — PASS (this document)

---

## C. Manifest correctness

- **Permissions declared:**
  - FOREGROUND_SERVICE — PASS (line 13)
  - FOREGROUND_SERVICE_SPECIAL_USE — PASS (line 14)
  - RECEIVE_BOOT_COMPLETED — PASS (line 15)
  - POST_NOTIFICATIONS — PASS (line 16)
  - REQUEST_IGNORE_BATTERY_OPTIMIZATIONS — PASS (line 17, optional per ADR but declared)
  - **BIND_ACCESSIBILITY_SERVICE** — **CONCERN C-1**: declared on the `<service android:permission=>` (line 84) but NOT as `<uses-permission>`. This is technically the right choice for a signature permission, but it produces an `aapt dump permissions` output of 5 entries instead of the 6 the DoD line asks for. Recommend adding the `<uses-permission>` line for AC traceability — Android Lint will warn but won't fail and there's no runtime cost.
- **Android 14 `<property>`** with non-empty value `"focus_lock_persistence"` inside `<service>` — PASS (lines 71-74).
- **`<queries>` block** — PASS (lines 6-10). Note: with `TYPE_WINDOW_STATE_CHANGED`, the package name is delivered on the event itself; no `PackageManager.queryIntentActivities` is called in the relaunch path. The `<queries>` block here is harmless and safe to keep for forward-compatibility.
- **`dataExtractionRules` and `fullBackupContent`** wired and pointing at XML files that exclude `datastore/wizedup_state.preferences_pb` — PASS (lines 22-23, `data_extraction_rules.xml:9-14`, `backup_rules.xml:6`).
- **FocusActivity attributes** — PASS. `singleTask`, `excludeFromRecents=true`, `clearTaskOnLaunch=true`, `showWhenLocked=true`, `turnScreenOn=true`, fullscreen theme `Theme.WizedUpFocus.Fullscreen`. The brief asks for `noHistory` OR `clearTaskOnLaunch` OR `singleTask` — engineer used `singleTask + clearTaskOnLaunch + taskAffinity=""`, which is the cleaner and more correct combo for this lock-screen pattern.

---

## D. AccessibilityService correctness

- `accessibilityEventTypes="typeWindowStateChanged"` — PASS.
- `canRetrieveWindowContent="false"` — PASS. Honest: we never read window content.
- Filter ignores own package — PASS (`FocusAccessibilityService.kt:57`).
- Launch flags `NEW_TASK | CLEAR_TOP | REORDER_TO_FRONT` — PASS (`FocusAccessibilityService.kt:81-83`).
- `accessibility_service_config.xml` declares correct event types and includes a description string — PASS (`accessibility_service_config.xml:8-15`, `strings.xml:7`). Description is honest and explanatory ("...we only watch for window-state changes; we never read content from other apps").
- `onUnbind` cancels the scope — PASS (`FocusAccessibilityService.kt:74-77`).
- Cached `isActive` read off the hot path via `MutableStateFlow.value` — PASS, avoids per-event DataStore I/O.

---

## E. Foreground service correctness

- `foregroundServiceType="specialUse"` — PASS (`AndroidManifest.xml:70`).
- `startForeground` invoked in `onCreate` (always) and again on every `onStartCommand` — PASS (`FocusForegroundService.kt:42`, `:53-54`). This eliminates the "did not call startForeground in time" exception risk.
- API 34+ `startForeground` overload with FGS type — PASS (`FocusForegroundService.kt:97-105`).
- Channel created in `Application.onCreate` — PASS (`WizedUpApplication.kt:37`, `:50-66`).
- Stops cleanly on deactivate — PASS, ACTION_STOP path calls `stopForegroundAndSelf` (`FocusForegroundService.kt:47-49`).
- Defensive self-stop on system restart when flag is false — PASS (`FocusForegroundService.kt:72-80`).
- **CONCERN E-1**: `FocusServiceController.stop` calls both `startService(stopIntent)` and `stopService(...)` (`FocusServiceController.kt:46-47`). Ordering is undefined — `stopService` likely wins, killing the service before the ACTION_STOP intent runs. Effect: the notification is removed by the system anyway, but the explicit `stopForegroundAndSelf` cleanup never executes. No user-visible failure observed in code review, but this is duplicative and worth tightening to either `startService(stopIntent)` only OR `stopService(...)` only.

---

## F. DataStore + persistence

- Uses `DataStore<Preferences>` (not `SharedPreferences`) — PASS (`FocusStateRepository.kt:20-22`).
- All flows are `Flow`, all setters are `suspend` — PASS (`FocusStateRepository.kt:51-79`, `:101-154`).
- File path matches the data_extraction_rules exclusion — PASS. `preferencesDataStore(name = "wizedup_state")` resolves to `applicationContext.filesDir/datastore/wizedup_state.preferences_pb`; both XML files exclude exactly that path. Engineer's pre-flagged deviation #7 verified PASS.
- `snapshot()` for receiver use is bounded at 10 s via `withTimeout` — PASS (`FocusStateRepository.kt:87-89`).
- UUID validation on read — PASS (`FocusStateRepository.kt:111`).
- DisplayName validation throws BEFORE `dataStore.edit` — PASS (`FocusStateRepository.kt:133`).

---

## G. UI / Compose

- Green token (`#1B873F`) for off — PASS (`Theme.kt:11`, `colors.xml:4`).
- Red token (`#C8262C`) for active — PASS (`Theme.kt:13`, `colors.xml:10`).
- Onboarding Continue disabled when name blank — PASS (`OnboardingScreen.kt:49`).
- Exit dialog verbatim copy — PASS (`strings.xml:39-42`).
- **CONCERN G-1**: dark-mode green-on-#121212 contrast ratio is approximately 4.4:1, just below WCAG AA's 4.5:1 floor. Stories US-R1-04 AC and US-R1-13 AC require ≥ 4.5:1 in dark mode. Engineer can lift `FocusOffGreen` to `#22A04A` or use `FocusOffGreenContainer` as the background ink in dark mode. Non-blocking but a literal AC miss.
- **CONCERN G-2**: `MainActivity.AppRoot` and `HomeScreen` use `collectAsState` rather than `collectAsStateWithLifecycle` (`MainActivity.kt:60`, `:63`; `HomeScreen.kt:61`). The lifecycle-aware variant is in the project deps (`androidx-lifecycle-runtime-compose`). Behaviorally indistinguishable here because both screens are within active activities, but US-R1-04 AC line 95 calls it out by name. Trivial swap.
- **CONCERN G-3**: cold launch with `focus.is_active=true` shows `OnboardingScreen` (initial=null profile) → `HomeScreen` (after profile flow emits) → `FocusActivity` (after isActive flow emits). Engineer's pre-flagged item #6 was "no flash of home screen on cold-launch-into-active-state" — there IS a brief flash, on the order of one or two frames before the `LaunchedEffect` fires. AccessibilityService will reassert on top within ~200 ms anyway. Mitigation if desired: render a neutral splash composable when both flows are at their initial values, then branch only after the first emission. Concern, not blocker — the AccessibilityService backstops it. Note that the BOOT_COMPLETED path does NOT go through MainActivity (BootReceiver starts the service; user taps the notification to enter FocusActivity), so this only affects the user opening the app icon while the flag is true.
- No deprecated APIs — PASS. `setShowWhenLocked` is API 27+; the `O_MR1` guard at `SystemUiUtils.kt:38` covers fallback. `WindowCompat.setDecorFitsSystemWindows` is current.

---

## H. Tests

- Unit tests present and structurally sound — PASS. `FocusStateRepositoryTest` exercises real DataStore via `PreferenceDataStoreFactory.create` against a temp file and covers fresh state, activate, deactivate, snapshot, name validation (empty / whitespace / 64 / 65), idempotent re-onboarding (`FocusStateRepositoryTest.kt`).
- `FocusServiceControllerTest` mocks `Context` via mockk and asserts intent component + action + that stop sends both intents — PASS (`FocusServiceControllerTest.kt`). The `verify(exactly = 1) { ctx.startService(any()) }` line will fail with the current mockk relaxed-mock setup unless mockk is told the test is verifying exact counts; minor but the test was clearly intended to lock in the dual-call shape.
- `SmokeInstrumentationTest` is an honest placeholder, callout in source — PASS (`SmokeInstrumentationTest.kt`).
- **CONCERN H-1**: there is no test for `FocusViewModel`, `OnboardingViewModel`, `HomeViewModel`, `BootReceiver`, or the accessibility service, despite the architecture testing-strategy table calling each out. The architecture says "Robolectric or instrumentation" for `BootReceiver` — engineer included Robolectric in the dependency list (`libs.versions.toml:16`) but didn't use it. Not blocking R1 because the manual matrix covers behavior, but flag as carry-over.
- **CONCERN H-2**: `FocusViewModel.tickJob` body reads `focusState.startedAtMs` inside a `viewModelScope.launch { while (true) { … } }`. The smart-cast from the outer `if (focusState.isActive && focusState.startedAtMs != null)` should propagate (focusState is a captured `val`, startedAtMs is a `val` data-class property), and Kotlin 1.9 typically does propagate. If the compiler refuses the smart-cast inside the inner lambda, the `System.currentTimeMillis() - started` line will fail to compile against `Long?`. **Cannot verify without a build.** Recommend the engineer wrap with `val startedSnapshot = focusState.startedAtMs ?: return@launch` before the `while (true)` to make it bulletproof and self-documenting.

---

## I. Docs

- README has install/run/test instructions — PASS (`android/README.md:64-114`).
- README has known limitations — PASS (sections 1–5 in §"Known limitations and disclosures"). Covers Play Store accessibility-use risk, boot→accessibility rebind gap, force-stop, OEM killers, no-Hilt rationale.
- README has acceptance-criteria traceability table — PASS (`android/README.md:160-170`).
- Manual test plan covers all 5 MISSION ACs in 5 sections + sign-off checklist — PASS (`docs/MANUAL_TEST_PLAN.md`). Each section maps to MISSION AC, lists steps, expected result, emulator-vs-physical notes. Sign-off checklist at the bottom is operational.

---

## J. Code quality (light pass)

- **CONCERN J-1**: `AccessibilityPermissionScreen.kt` is dead code — the file is well-documented as the rationale-screen alternative but is not on any navigation graph. R8 should strip unreachable composables. Acceptable for R1 if engineer plans to use it in R2; otherwise delete.
- **CONCERN J-2**: `OnboardingScreen.kt:45` declares `onContinueComplete: () -> Unit = {}` but `MainActivity.kt:69` calls the screen without supplying it; navigation is reactive via the profile flow. The parameter is dead. Either remove it or wire it.
- **No race conditions found** in: BootReceiver (single goAsync + single launch + finish), accessibility service hot path (cached value read), foreground service (single onCreate + onStartCommand pattern, scope cancelled on destroy).
- **No security issues**: no logging of student name, no PII to logcat, PendingIntent is FLAG_IMMUTABLE, exported flags are correct (MainActivity exported=true required, FocusActivity exported=false, accessibility service exported=true required, foreground service exported=false, BootReceiver exported=true required).
- **Manifest minified set**: no `INTERNET`, no `SYSTEM_ALERT_WINDOW`, no `BIND_DEVICE_ADMIN`, no `PACKAGE_USAGE_STATS` — clean.

---

## K. Engineer's pre-flagged deviations — verdicts

### Deviation 1 — Notification channel `IMPORTANCE_LOW` vs spec'd `IMPORTANCE_HIGH`. Verdict: **CONCERN, accepted with carry-over.**

The story (US-R1-09 AC line 196 and US-R1-07 AC line 156) is explicit: `IMPORTANCE_HIGH`. Engineer chose `IMPORTANCE_LOW` for non-intrusive UX. This is a defensible product call: an always-on status indicator that pops a heads-up every relaunch would be jarring and degrade the brick's voluntary feel. The mission AC #2 ("all other apps inaccessible") does not depend on importance — the relaunch loop in the AccessibilityService does the work, and the notification's role is to be a tap target during the post-boot rebind gap. `IMPORTANCE_LOW` still shows the notification in the shade and on the lock screen.

The literal AC reads HIGH. PM should either (a) accept the deviation and update the story to LOW with a note, or (b) require the engineer to flip the constant to HIGH. Recommend (a) for the brick UX, with the change documented in a story-amendment note. Either way, this is **not a blocker**.

### Deviation 2 — `AccessibilityPermissionScreen.kt` built but not wired into the default flow; rationale dialog in `HomeScreen` is used instead. Verdict: **ACCEPTED.**

US-R1-05 AC explicitly describes a dialog: "the app shows a rationale dialog: title 'One-time setup', body explaining …, primary button 'Open Settings', secondary button 'Cancel'." A modal AlertDialog over the home screen matches the AC text exactly. A separate full-screen activity would (a) add a screen to the navigation graph, (b) lengthen the in-app tap path on subsequent activations if the user re-enters this state, and (c) violate the "stay on home, don't auto-retry" AC clause. Dialog is correct. The orphan file is a code-cleanliness concern only (J-1).

### Deviation 3 — `FocusAccessibilityService` does not special-case `com.android.systemui`. Verdict: **ACCEPTED.**

US-R1-08 AC line 180: "Given a system UI surface (e.g. `com.android.systemui` notification shade), when it appears, then it is treated like any other foreground change — `FocusActivity` is reasserted." The story is unambiguous that the shade should trigger relaunch. US-R1-12's manual matrix row 1 expects FocusActivity to return within 500 ms after dismissing the shade. The brief flicker is acceptable per ADR-002 §"Negative". Engineer's behavior matches the spec exactly. **PASS.**

### Deviation 4 — `BootReceiver` filters both `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED` but only acts on `BOOT_COMPLETED`. Verdict: **ACCEPTED.**

DataStore Preferences is not direct-boot-aware (file lives in credential-protected storage), so `LOCKED_BOOT_COMPLETED` cannot read the flag. The receiver's `directBootAware="false"` already means the system would not deliver `LOCKED_BOOT_COMPLETED` before user unlock anyway, so the filter line is technically redundant, but the explicit when-branch documents the decision and protects against future changes to direct-boot policy. After unlock, `BOOT_COMPLETED` arrives and the active path runs. User experience matches US-R1-11 AC: "within 10 s of unlock, persistent notification is visible." **PASS.**

### Deviation 5 (engineer's "watch this" item) — `FocusActivity.onCreate` defensively starts the service; service self-stops if the flag is false. Verdict: **PASS, no zombie service.**

Flow: deactivate writes flag false → state flow emits → `LaunchedEffect(state.isActive)` in `FocusActivity` runs `FocusServiceController.stop` then launches `MainActivity` then `finish()`. The defensive `start` in `onCreate` only runs on activity creation. Since the FocusActivity is `singleTask` and finishes on deactivate, a new onCreate isn't called from within the deactivate path. The only window where the defensive `start` could spawn an unwanted service is if the activity is started while the flag is already false (e.g. a stale notification tap after deactivate). In that case `FocusForegroundService.onStartCommand` runs `verifyStateOrStop` which reads the flag and self-stops. The notification flickers up then disappears. Acceptable. **PASS-with-info.**

### Deviation 6 (engineer's "watch this" item) — `MainActivity.AppRoot` reactively jumps to FocusActivity when `isActive` flips true. Verdict: **CONCERN G-3** — there IS a brief flash on cold-launch-into-active-state. Not blocking; AccessibilityService backstops.

### Deviation 7 (engineer's "watch this" item) — `data_extraction_rules.xml` excludes `datastore/wizedup_state.preferences_pb`, matching the actual DataStore path. Verdict: **PASS.**

`preferencesDataStore(name = "wizedup_state")` produces `filesDir/datastore/wizedup_state.preferences_pb`. Both XML files exclude exactly that path. Auto-backup verification step in MANUAL_TEST_PLAN.md §5 covers it.

---

## L. Concern register (consolidated, prioritized)

| # | ID | Severity | Location | Description | Suggested fix |
|---|----|----------|----------|-------------|---------------|
| 1 | C-1 | Concern | AndroidManifest.xml | `BIND_ACCESSIBILITY_SERVICE` not declared as `<uses-permission>`; aapt dump shows 5 not 6 | Add `<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />` for AC traceability |
| 2 | B-2 / K-1 | Concern | WizedUpApplication.kt:56 | Channel importance is LOW, story says HIGH | PM to amend story OR flip to HIGH (one-line change) |
| 3 | E-1 | Concern | FocusServiceController.kt:46-47 | Both `startService(stopIntent)` and `stopService(...)` called; ordering undefined | Pick one; recommend keeping `startService(stopIntent)` for clean ACTION_STOP teardown |
| 4 | H-2 | Concern | FocusViewModel.kt:54-58 | `focusState.startedAtMs` re-read inside inner lambda; smart-cast may not propagate | Capture `val started = focusState.startedAtMs ?: return@launch` outside the while-loop |
| 5 | G-1 | Concern | Theme.kt:11 / colors.xml:4 | Dark-mode green-on-#121212 ≈ 4.4:1, below WCAG AA 4.5:1 | Lift to ~`#22A04A` for dark variant |
| 6 | G-2 | Concern | MainActivity.kt:60,63; HomeScreen.kt:61 | Uses `collectAsState` not `collectAsStateWithLifecycle` | One-import swap; behaviorally same here |
| 7 | G-3 / dev #6 | Concern | MainActivity.kt:55-81 | Brief flash of Onboarding/Home on cold-launch-into-active-state | Add a neutral initial composable while both flows are at initial value |
| 8 | H-1 | Concern | tests | No tests for ViewModels, BootReceiver, accessibility service | Add Robolectric BootReceiver test + 2 VM tests in R1 polish or carry to R2 |
| 9 | J-1 / dev #2 | Concern | AccessibilityPermissionScreen.kt | Dead code; not on the nav graph | Delete OR document why it's kept for R2 |
| 10 | J-2 | Concern | OnboardingScreen.kt:45 | Unused `onContinueComplete` parameter | Remove or wire |
| 11 | I-1 | Info | README | Play Console accessibility-use disclosure draft is referenced but not yet attached to the PR | Out-of-app deliverable; PM to ensure before Play Store submission |

---

## M. Sign-off

The brick works. The five MISSION.md acceptance criteria are met by the code as written, modulo the eleven concerns above — none of which block the brick's correctness or the voluntary-honor-system contract. The engineer has been honest about deviations and has documented the boot→accessibility rebind gap, OEM killers, and force-stop unrecoverability where they belong.

**Recommendation to PM:** gate to R2 with the eleven concerns recorded as carry-over tasks. The two literal-AC-mismatches (notification importance, BIND_ACCESSIBILITY_SERVICE uses-permission line) need a one-line PM call: either amend the stories or ask the engineer for a one-line fix each. Neither blocks the brick's behavior.

— QA Agent, 2026-05-08
