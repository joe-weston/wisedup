# MISSION.md — WisedUp Focus App

## Project Overview

WisedUp is a **voluntary, accountability-first focus app** for students. Unlike enforcement-based tools, WisedUp works on the honor system — students choose to activate focus mode when entering school. The app's value proposition is **transparent logging**: every activation, deactivation, and bypass attempt is recorded and surfaced to school administrators, turning phone policy from a he-said/she-said argument into a data-driven conversation.

**Core Philosophy:**
- Voluntary activation only — no MDM, no forced enrollment
- Full transparency — students know everything is logged
- Accountability over punishment — data informs conversations, not automatic consequences
- Privacy-respecting — logs behavior events only, not content

---

## Agent Roles & Responsibilities

| Agent | Role |
|---|---|
| **Project Manager** | Orchestrates the swarm, tracks phase completion, gates progression between releases, maintains memory state |
| **Workflow Expert** | Produces Mermaid diagrams for each release's user flows and system interactions |
| **Architect** | Defines technical architecture, selects libraries, designs data models, writes ADRs (Architecture Decision Records) |
| **Product Manager** | Writes user stories with acceptance criteria from the Architect's designs |
| **Engineer** | Executes user stories — writes, tests, and commits code |
| **QA Agent** | Reviews Engineer output against acceptance criteria before PM gates to next phase |

**Execution Rule:** No phase advances until QA signs off. PM holds the gate.

---

## Release 1 — Android Brick (Core Focus Mode)

**Goal:** A working Android app that students can voluntarily activate to block access to all other apps. No backend. No logging. Just the brick.

### Scope
- Splash / onboarding screen (student name or ID entry — stored locally only for R1)
- **Focus Mode ON** — locks device to a single "You're focused" screen
- Student can exit at any time (voluntary) via a clearly labeled button
- App survives: screen lock, notification pull-down, app switcher, reboot
- Visual state is clear: locked = red/amber "Focus Active", unlocked = green "Focus Off"

### Technical Constraints
- Native Android (Kotlin preferred, or React Native with native modules)
- Uses Android Accessibility Service or DevicePolicyManager for app blocking
- Targets Android 10+ (API 29+)
- No network calls in R1

### Acceptance Criteria
- [ ] Student can open app and activate Focus Mode in ≤ 2 taps
- [ ] All other apps are inaccessible during Focus Mode
- [ ] Focus Mode survives device reboot
- [ ] Student can voluntarily exit at any time
- [ ] App does not require school IT involvement to install

---

## Release 2 — Compliance Logging

**Goal:** Every Focus Mode event is logged to a Supabase backend. The Android app from R1 gains network awareness.

### Scope
- Supabase project setup (schema below)
- Student "registration" — simple ID/name + school code on first launch
- Log events: `focus_started`, `focus_ended`, `bypass_attempted`
- Log metadata: `timestamp`, `student_id`, `school_id`, `device_id`, `duration_seconds`
- Offline-first: queue events locally (SQLite) and sync when connected
- No auth UI for students in R2 (school code = implicit org association)

### Supabase Schema (draft — Architect to validate)
```sql
schools     (id, name, code, created_at)
students    (id, school_id, display_name, device_id, created_at)
focus_sessions  (id, student_id, started_at, ended_at, duration_seconds)
bypass_events   (id, student_id, session_id, event_type, timestamp)
```

### Acceptance Criteria
- [ ] All focus events logged within 5 seconds of occurrence (when online)
- [ ] Events queued and synced correctly after offline period
- [ ] No PII beyond student display name and school-assigned ID
- [ ] Supabase RLS policies prevent students from reading other students' data

---

## Release 3 — Admin Dashboard

**Goal:** A web dashboard for school administrators to view compliance data from R2.

### Scope
- Admin login (Supabase Auth — email/password)
- School-level view: daily/weekly compliance rate across all students
- Student-level view: individual focus session history, bypass events
- Key metrics: average session length, compliance %, bypass frequency
- Date range filter
- CSV export of any view

### Tech Stack
- Next.js + Tailwind (or SvelteKit — Architect decides)
- Hosted on Vercel or Supabase Edge
- Reads only from Supabase (no direct device communication)

### Acceptance Criteria
- [ ] Admin can log in and see their school's data only
- [ ] Dashboard loads in < 2s for up to 500 students
- [ ] All charts accurate against raw Supabase data
- [ ] CSV export includes all displayed fields

---

## Release 4 — iOS Port

**Goal:** Feature-complete iOS version of the R1+R2 Android app, connecting to the same R2/R3 backend.

### Scope
- Native iOS app (Swift / SwiftUI preferred) or React Native port of Android codebase
- Same focus mode behavior using iOS Screen Time API (`ManagedSettings` framework)
- Same logging schema and Supabase integration as R2
- Same onboarding flow

### Technical Constraints
- Targets iOS 16+
- **Known limitation:** iOS Screen Time API requires `com.apple.developer.family-controls` entitlement — Architect must document the App Store submission requirements and any TestFlight distribution path
- Focus mode on iOS cannot fully replicate Android's device admin approach — Architect to document delta and propose mitigation

### Acceptance Criteria
- [ ] iOS app logs to same Supabase backend as Android
- [ ] Focus mode blocks app access on iOS (within platform constraints)
- [ ] Admin dashboard displays iOS and Android sessions without distinction
- [ ] Architect ADR documents iOS platform limitations clearly

---

## Phase Gate Rules (for PM Agent)

```
R1 complete → QA signs off on brick reliability    → proceed to R2
R2 complete → QA validates events in Supabase      → proceed to R3
R3 complete → QA validates dashboard accuracy      → proceed to R4
R4 complete → QA validates iOS parity w/ Android   → MVP complete
```

---

## Memory Keys (for Ruflo swarm state)

```
mission/current_release
mission/current_phase          # architecture | stories | build | qa
mission/blocking_issues
architecture/android_approach
architecture/supabase_schema_version
architecture/dashboard_stack
```

---

## Out of Scope (all releases)

- Forced enrollment or MDM
- Monitoring of app content or messages
- Geofencing / automatic school detection
- Gamification or rewards
- Parent portal (post-MVP)
