# R1 Android Brick — Workflow Diagrams

Technology-agnostic workflow documentation for Release 1 (Android Brick / Core Focus Mode). The Architect will resolve native-vs-RN and AccessibilityService-vs-DevicePolicyManager separately; these diagrams use generic component names (`FocusService`, `DeviceLockComponent`, `LocalStore`) so they remain valid under either choice.

Scope reminder: voluntary activation, locked single-screen UI blocking other apps, voluntary exit, survives reboot, no backend, no logging, no network.

---

## 1. User Flow

**What it shows:** The end-to-end student journey from first launch (ID entry) through home screen, activation, the locked focus state, and voluntary exit back to home.

```mermaid
flowchart TD
    Start([Student opens app]) --> FirstLaunch{First launch?}
    FirstLaunch -- Yes --> Onboard[Onboarding screen<br/>Enter name / student ID]
    Onboard --> SaveID[Save ID to local store]
    SaveID --> Home
    FirstLaunch -- No --> Home[Home screen<br/>Status: Focus Off green<br/>Button: Activate Focus]
    Home --> Tap1[Tap Activate Focus]
    Tap1 --> PermCheck{Required device<br/>permissions granted?}
    PermCheck -- No --> PermPrompt[Prompt user to grant<br/>device lock permission]
    PermPrompt --> PermResult{Granted?}
    PermResult -- No --> Home
    PermResult -- Yes --> Activate
    PermCheck -- Yes --> Activate[Activate Focus<br/>persist focus_active = true]
    Activate --> Locked[Locked screen<br/>Status: Focus Active red/amber<br/>You're focused message<br/>Exit Focus button]
    Locked --> Attempt{Student action}
    Attempt -- Home / app switcher /<br/>notification pull-down --> Locked
    Attempt -- Screen lock then unlock --> Locked
    Attempt -- Tap Exit Focus --> Confirm[Confirm voluntary exit]
    Confirm -- Cancel --> Locked
    Confirm -- Confirm --> Deactivate[Deactivate<br/>persist focus_active = false]
    Deactivate --> Home
```

---

## 2. State Diagram

**What it shows:** The app's lifecycle states and transitions, including the boot-survival path where a reboot during `Active` re-enters `Active` via the boot receiver instead of dropping back to `Idle`.

```mermaid
stateDiagram-v2
    [*] --> Onboarding: App installed,<br/>no stored ID
    Onboarding --> Idle: ID saved to local store
    [*] --> Restoring: App launched,<br/>ID already stored

    Restoring --> Idle: focus_active = false
    Restoring --> Active: focus_active = true<br/>(resume after reboot)

    Idle --> Active: Tap Activate Focus<br/>+ permissions granted<br/>(visual: green to red/amber)
    Active --> Idle: Tap Exit Focus<br/>+ confirm<br/>(visual: red/amber to green)

    state Active {
        [*] --> Locked
        Locked --> Locked: Home / recents /<br/>notif pull-down /<br/>screen lock-unlock<br/>(re-assert lock)
    }

    Idle --> [*]: App killed<br/>(no persistence needed)
    Active --> Rebooting: Device reboot
    Rebooting --> Active: BootReceiver fires<br/>reads focus_active = true<br/>re-starts FocusService
```

---

## 3. Sequence Diagram

**What it shows:** Runtime interactions between the student, MainActivity, FocusService, the device lock component, the local store, and BootReceiver during activation, ongoing blocking, voluntary exit, and reboot recovery.

```mermaid
sequenceDiagram
    actor Student
    participant UI as MainActivity (UI)
    participant Store as LocalStore
    participant Svc as FocusService
    participant Lock as DeviceLockComponent
    participant Boot as BootReceiver

    Note over Student,Boot: Activation
    Student->>UI: Tap Activate Focus
    UI->>Lock: Check permissions
    Lock-->>UI: Granted
    UI->>Store: set focus_active = true
    UI->>Svc: start()
    Svc->>Lock: enable lock / pin to focus screen
    Lock-->>Svc: locked
    Svc-->>UI: state = Active (red/amber)
    UI-->>Student: Show locked focus screen

    Note over Student,Boot: Bypass attempts while Active
    Student->>Lock: Press Home / Recents / pull notifications
    Lock->>Svc: foreground event
    Svc->>Lock: re-assert focus screen
    Lock-->>Student: Returned to focus screen

    Note over Student,Boot: Voluntary exit
    Student->>UI: Tap Exit Focus, confirm
    UI->>Svc: stop()
    Svc->>Lock: release lock
    UI->>Store: set focus_active = false
    Svc-->>UI: state = Idle (green)
    UI-->>Student: Show home screen

    Note over Student,Boot: Reboot recovery
    Student->>Boot: Device reboot completes
    Boot->>Store: read focus_active
    Store-->>Boot: true
    Boot->>Svc: start()
    Svc->>Lock: re-enable lock
    Lock-->>Student: Locked focus screen restored
```

---

## 4. Component Interaction

**What it shows:** Static wiring between the four R1 components — `MainActivity` (UI), `FocusService` (focus lifecycle), `BootReceiver` (reboot survival), and `LocalStore` (persisted ID + focus flag) — plus their relationship to the generic `DeviceLockComponent` that abstracts whichever Android lock API the Architect picks.

```mermaid
flowchart TD
    subgraph App[WizedUp R1 App]
        UI[MainActivity<br/>onboarding + home + locked UI]
        Svc[FocusService<br/>start / stop / re-assert]
        Boot[BootReceiver<br/>BOOT_COMPLETED handler]
        Store[(LocalStore<br/>student_id, focus_active)]
    end

    Lock[[DeviceLockComponent<br/>AccessibilityService or<br/>DevicePolicyManager - TBD by Architect]]
    OS[(Android OS<br/>boot, foreground events,<br/>system UI)]

    UI -- read/write student_id, focus_active --> Store
    UI -- start / stop --> Svc
    Svc -- read focus_active --> Store
    Svc -- enable / disable / re-assert --> Lock
    Lock -- foreground events --> Svc

    OS -- BOOT_COMPLETED broadcast --> Boot
    Boot -- read focus_active --> Store
    Boot -- start (if active) --> Svc

    OS -- system UI events --> Lock
    UI -- render state color --> UI
```

---

## Notes for Reviewers

- All four diagrams treat the locking mechanism as a single abstract `DeviceLockComponent`. Swapping in AccessibilityService vs DevicePolicyManager (or a hybrid) does not change the flows.
- `LocalStore` is shown generically. SharedPreferences, DataStore, or a small SQLite table all satisfy R1.
- Permission flow is shown only at activation time; the Architect may choose to also request at first launch.
- "Re-assert lock" in the sequence diagram covers the survival requirements (home, recents, notification pull-down, screen lock/unlock).
