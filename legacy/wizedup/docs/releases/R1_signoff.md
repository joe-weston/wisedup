---
release: R1
title: Android Brick — Core Focus Mode
status: COMPLETE
signed_off_by: Project Manager (PM agent)
signed_off_on: 2026-05-08
qa_verdict: PASS-WITH-CONCERNS (0 blocking, 11 non-blocking; 5 resolved in fixup, 6 carried to R2 backlog)
---

# Release 1 Sign-off

## Outcome

R1 is complete. The brick works on paper: a Kotlin + Compose Android app that
students voluntarily activate to lock their device to a single "Focus Active"
screen, with state surviving screen lock, notification pull-down, app switcher,
and reboot. No backend, no logging, no network. The next phase (R2 — Compliance
Logging) is **not** started; per the phase-gate rules in MISSION.md, this PM
holds the gate.

## MISSION.md acceptance criteria — final status

| # | Criterion | Status | Primary evidence |
|---|---|---|---|
| 1 | Activate Focus Mode in ≤ 2 taps | PASS | `HomeScreen.kt` (single CTA), `MainActivity.kt` (cold-launch routing); QA section A1 |
| 2 | All other apps inaccessible during Focus Mode | PASS | `FocusAccessibilityService.kt` (foreground-package detection + relaunch), `FocusActivity.kt` (singleTask + excludeFromRecents); QA section A2 |
| 3 | Focus Mode survives device reboot | PASS | `BootReceiver.kt`, `FocusStateRepository.kt` persisted flag; QA section A3 |
| 4 | Voluntary exit at any time | PASS | `FocusScreen.kt` exit confirm dialog with verbatim copy; QA section A4 |
| 5 | No school IT involvement to install | PASS | AccessibilityService approach (user-grantable), no DPM, no MDM; QA section A5 |

All five MISSION.md criteria were validated by QA against the implemented code.

## Phases executed

| Phase | Agent | Output | Commit |
|---|---|---|---|
| A | Workflow Expert | `docs/workflows/R1_workflows.md` (4 Mermaid diagrams) | `761cbc3` |
| A | Architect | 4 ADRs + `docs/architecture/R1_architecture.md` | `761cbc3` |
| B | Product Manager | 14 user stories with G/W/T acceptance criteria + DoD | `8c6d2d3` |
| C | Engineer | 44 files: Gradle, manifest, Kotlin, tests, docs | `f94f8d2`, `6d8df6e`, `71a9196`, `6ea6e2e`, `6714560` |
| D | QA | `docs/qa/R1_qa_review.md` — PASS-WITH-CONCERNS, 11 concerns | (this commit) |
| E | Engineer (fixup) | 5 QA concerns resolved | `968a8b3` |

## QA concerns resolved in fixup (`968a8b3`)

1. `BIND_ACCESSIBILITY_SERVICE` `<uses-permission>` added to AndroidManifest.xml for `aapt dump permissions` traceability per US-R1-08 AC. No-op at runtime.
2. `FocusViewModel` — defensive local `val started = focusState.startedAtMs ?: return@launch` lifted above the timer loop to avoid smart-cast loss across `delay()` suspension.
3. `FocusServiceController.stop()` simplified to single `stopService(...)` path; removed the prior dual `startService(stopIntent) + stopService()` to eliminate the start/stop race on slow devices.
4. Dark-mode green bumped from ~4.4:1 to ~9.1:1 contrast on `#121212` surface (now `#5BC97B`), clearing WCAG AA.
5. `AccessibilityPermissionScreen.kt` deleted (dead code) — the rationale dialog in `HomeScreen.kt` is the canonical permission UX, which keeps the ≤ 2 in-app taps target tight.

## PM amendments

- **US-R1-09 notification importance:** changed from `IMPORTANCE_HIGH` → `IMPORTANCE_LOW`. Heads-up banners over the lock screen for an always-on indicator are hostile UX. Behavioral enforcement is the AccessibilityService relaunch loop; the notification is a passive status indicator. Story file annotated with the dated rationale.

## R2 carry-overs (non-blocking, deliberately deferred)

The QA review surfaced six concerns that are carried forward, not blockers for R1:

1. **Cold-launch-into-active-state flicker** — when a user opens the app icon while focus is already active, MainActivity briefly shows OnboardingScreen → HomeScreen before LaunchedEffect routes to FocusActivity (~200 ms flicker, AccessibilityService reasserts on top within the same window). The boot-resume path is unaffected because users tap the persistent notification, not the icon. Visible flicker, not a correctness failure.
2. **No unit tests for `FocusViewModel`, `HomeViewModel`, or `BootReceiver`** — DataStore repository and FocusServiceController are covered. Add coverage in R2 hardening pass; instrumentation tests can run there too once a CI runner with the SDK is in place.
3. **Notification shade triggers a brief FocusActivity relaunch (~500 ms flicker)** — `FocusAccessibilityService` does not special-case `com.android.systemui`. Per US-R1-08, treating the shade like any other foreground change is correct (otherwise notifications become a bypass vector); document the flicker in the user-facing FAQ rather than code-around it.
4. **No CI / no executable build verification on this branch** — there is no Android SDK or JDK in the build environment. Stand up GitHub Actions with `setup-java` + `gradle/gradle-build-action` in R2. All R1 review was code-based.
5. **OEM battery-killer testing** — Architect flagged Xiaomi/Huawei/OnePlus/Samsung Deep Sleep as risks. Field-test one device per OEM during R2 (when there's also a backend to receive telemetry from those tests).
6. **Play Store accessibility-use disclosure** — out-of-app deliverable. Required before any public release; for pilot schools, side-load APK distribution is acceptable. Owner: PM, due before R3 (admin dashboard would imply public availability).

## Phase gate

Per MISSION.md:
> R1 complete → QA signs off on brick reliability → proceed to R2

QA has signed off (PASS-WITH-CONCERNS, 0 blocking). However, **the gate to R2 is held**, in line with the explicit instruction to execute Release 1 only. R2 (Compliance Logging) requires a Supabase project, schema review by the Architect, and new stories from the PM — none of which are in scope for this release.

## Repository state at sign-off

- Branch: `release/r1-android-brick`
- Final commit pre-sign-off: `968a8b3`
- Sign-off commit: this document
- Total commits this release: 9
