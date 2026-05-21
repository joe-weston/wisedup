Release 4 — iOS Port
Goal: Feature-complete iOS version of the R1+R2 Android app, connecting to the same R2/R3 backend.
Scope

Native iOS app (Swift / SwiftUI preferred) or React Native port of Android codebase
Same focus mode behavior using iOS Screen Time API (ManagedSettings framework)
Same logging schema and Supabase integration as R2
Same onboarding flow

Technical Constraints

Targets iOS 16+
Known limitation: iOS Screen Time API requires com.apple.developer.family-controls entitlement — Architect must document the App Store submission requirements and any TestFlight distribution path
Focus mode on iOS cannot fully replicate Android's device admin approach — Architect to document delta and propose mitigation

Acceptance Criteria

 iOS app logs to same Supabase backend as Android
 Focus mode blocks app access on iOS (within platform constraints)
 Admin dashboard displays iOS and Android sessions without distinction
 Architect ADR documents iOS platform limitations clearly


Phase Gate Rules (for PM Agent)
R1 complete → QA signs off on brick reliability → proceed to R2
R2 complete → QA validates all events appear in Supabase → proceed to R3  
R3 complete → QA validates dashboard accuracy against raw data → proceed to R4
R4 complete → QA validates iOS parity with Android → project MVP complete

Memory Keys (for Ruflo swarm state)
mission/current_release
mission/current_phase        # architecture | stories | build | qa
mission/blocking_issues
architecture/android_approach
architecture/supabase_schema_version
architecture/dashboard_stack

Out of Scope (for all releases)

Forced enrollment or MDM
Monitoring of app content or messages
Geofencing / automatic school detection
Gamification or rewards
Parent portal (post-MVP)