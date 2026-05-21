# WizedUp iOS

Placeholder for Release 4. The intended app is native Swift/SwiftUI and will connect to the same Supabase schema as Android.

Before scaffolding the Xcode project, write the R4 ADR covering:

- Family Controls entitlement requirements
- TestFlight and App Store review constraints
- `FamilyControls`, `ManagedSettings`, and `DeviceActivity` responsibilities
- Behavioral differences from Android Accessibility Service blocking
- Shared logging contract with `focus_sessions` and `bypass_events`
