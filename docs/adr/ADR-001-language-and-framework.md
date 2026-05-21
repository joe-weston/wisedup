# ADR-001: Language and Framework for R1 Android App

## Status
Accepted — 2026-05-08

## Context

Release 1 is Android-only and ships a "brick" focus app: a single foreground experience that blocks navigation to other apps, survives reboot, and exposes a voluntary exit. The work touches deep Android system surfaces — `AccessibilityService`, foreground `Service`, `BootReceiver`, `DevicePolicyManager` (rejected; see ADR-002), notification channels, and tight lifecycle handling.

R4 ships an iOS port. The temptation is to choose React Native now to "save work later." We need to decide deliberately, because the wrong choice front-loads bridge friction onto the most system-heavy release in the project.

Considerations:
- R1 is the highest-risk release technically (system integration, lifecycle).
- R2 adds Supabase networking — trivial in either stack.
- R4 (iOS) uses an entirely different blocking primitive (`ManagedSettings` / Screen Time API). Shared code at the blocking layer is essentially zero regardless of stack.
- Team has no current cross-platform RN bridge expertise documented.

## Decision

**Use native Kotlin with Jetpack (Compose for UI, DataStore for persistence, AndroidX for lifecycle).**

- `minSdk = 29` (Android 10), `targetSdk = 34` (Android 14).
- Kotlin 1.9.x, AGP 8.5.x, Gradle 8.7+.
- UI in Jetpack Compose; no XML layouts unless required by `AccessibilityService` config.
- No cross-platform abstraction layer in R1.

## Consequences

### Positive
- Direct, idiomatic access to `AccessibilityService`, `DevicePolicyManager`, foreground `Service`, `BroadcastReceiver`, and `WindowManager`. No bridge module to author or debug.
- Smallest possible APK and fastest cold start — important when the lock screen must relaunch quickly after a backgrounding event.
- Compose makes the two-tap onboarding and the locked screen (single composable, state-driven color) trivially small.
- Clean handoff to R2: Supabase has a first-class Kotlin SDK (`supabase-kt`).
- Easier hiring / community help for Android-specific edge cases.

### Negative
- R4 iOS port is a full rewrite of the UI and onboarding (~1–2 weeks of duplicated UI work). The blocking layer would have been a rewrite anyway.
- Two codebases to maintain from R4 onward. Mitigation: keep business logic (event schema, sync, validation) thin and mirrorable.

### Neutral
- Team learns Compose if they don't know it. Low risk; it is now the Android default.

## Alternatives Considered

### React Native + native modules
- **Pros:** One UI codebase across R1 and R4. Shared onboarding logic.
- **Cons:** All R1 system integration (accessibility service, boot receiver, foreground service, lock activity relaunch) lives in a custom native module anyway. The RN bridge adds a serialization hop on every state transition between the lock screen and the service — the exact hot path. Debugging accessibility-event handling across the JS bridge is painful. R4's iOS blocker uses a different API surface, so the "shared" win is UI only, which Compose Multiplatform or a future rewrite can capture cheaper.
- **Verdict:** Rejected. The cross-platform argument is real but applies to R4, not R1, and R1's risk is system depth.

### Flutter
- **Pros:** Single codebase, good performance.
- **Cons:** Same bridge problem as RN for accessibility services; smaller Android-systems community than Kotlin; Supabase Dart SDK is less mature than Kotlin's; nobody on the team is asking for it.
- **Verdict:** Rejected.

### Kotlin Multiplatform Mobile (KMM)
- **Pros:** Share business logic to iOS in R4 while keeping native UI on each platform.
- **Cons:** Premature for R1 — there is no shared business logic yet. Adopt in R2 if the sync layer warrants it.
- **Verdict:** Defer. Revisit at R2 planning.

## iOS (R4) Context — Read This Before You're Surprised

R4 will not reuse R1's blocking code regardless of stack choice. iOS uses `ManagedSettings` / `DeviceActivity` / `FamilyControls` — a fundamentally different model (system-mediated, requires Family Controls entitlement, no equivalent of `AccessibilityService`). The R4 ADR will own that delta. What we *can* share across R1 and R4:

- Event schema and sync protocol (R2 onward).
- Onboarding copy, visual design tokens, and UX flows.
- Supabase client patterns.

What we cannot share: the focus-mode enforcement layer. Choosing RN to "save" that work is a false economy.

## Related Decisions
- ADR-002: Blocking mechanism (depends on native APIs from this decision).
- ADR-003: Persistence and reboot (uses Android `BroadcastReceiver`).
- ADR-004: Local data model (uses Jetpack DataStore).

## References
- Android Accessibility Service: https://developer.android.com/guide/topics/ui/accessibility/service
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Supabase Kotlin: https://github.com/supabase-community/supabase-kt
