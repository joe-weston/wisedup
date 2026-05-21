Release 1 — Android Brick (Core Focus Mode)
Goal: A working Android app that students can voluntarily activate to block access to all other apps. No backend. No logging. Just the brick.

**QA / verification:** [docs/MANUAL_TEST_PLAN.md](docs/MANUAL_TEST_PLAN.md)

Scope

Splash / onboarding screen (student name or ID entry — stored locally only for R1)
Focus Mode ON — locks device to a single "You're focused" screen
Student can exit at any time (voluntary) via a clearly labeled button
App survives: screen lock, notification pull-down, app switcher, reboot
Visual state is clear: locked = red/amber "Focus Active", unlocked = green "Focus Off"

Technical Constraints

Native Android (Kotlin preferred, or React Native with native modules)
Uses Android Accessibility Service or DevicePolicyManager for app blocking
Targets Android 10+ (API 29+)
No network calls in R1

Acceptance Criteria

How we measure “≤ 2 taps” (AC #1): One-time first-launch onboarding (name/ID + Continue) and the one-time trip to system Settings to enable the accessibility service are **excluded** from the tap count. The metric applies **from the home screen after setup**. With permissions already granted, activating Focus from the home screen is **one in-app tap** on subsequent launches; at most **two in-app taps** when an extra step is required (e.g. notification permission on first activate). See Section 1 of the manual test plan.

 Student can open app and activate Focus Mode in ≤ 2 taps (per measurement above)
 All other apps are inaccessible during Focus Mode (see Limitations below)
 Focus Mode survives device reboot
 Student can voluntarily exit at any time
 App does not require school IT involvement to install

Limitations (R1)

- **Not a tamper-proof jail:** A student can open Settings and disable the accessibility service. That ends the brick by design (voluntary / honor-system model); R2 will log bypass attempts. During normal use with the service enabled, other apps are covered by the relaunch loop (see [docs/adr/ADR-002-blocking-mechanism.md](docs/adr/ADR-002-blocking-mechanism.md)).
- **Reboot bridge:** Until the accessibility service rebinds, the lock may rely on the foreground-service notification and/or an immediate resume activity where the OS allows it; see [docs/adr/ADR-003-persistence-and-reboot.md](docs/adr/ADR-003-persistence-and-reboot.md).
