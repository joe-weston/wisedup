Release 1 — Android Brick (Core Focus Mode)
Goal: A working Android app that students can voluntarily activate to block access to all other apps. No backend. No logging. Just the brick.
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

 Student can open app and activate Focus Mode in ≤ 2 taps
 All other apps are inaccessible during Focus Mode
 Focus Mode survives device reboot
 Student can voluntarily exit at any time
 App does not require school IT involvement to install