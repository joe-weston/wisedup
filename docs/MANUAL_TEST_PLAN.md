# Manual Test Plan — R1 Android Brick

**Audience:** QA agent and pilot-school IT contacts.
**Scope:** all five MISSION.md R1 acceptance criteria + the survival matrix from US-R1-12.
**Reference devices:** Pixel 6 emulator (API 34) + at least one physical device per OEM
(Samsung, Xiaomi, OnePlus). The acceptance criteria are passable on emulator alone; OEM
devices are recommended for the survival matrix because of vendor-specific killers
(see ADR-003).

Each section maps to one MISSION.md acceptance criterion. Run sections 1–5 in order; each
assumes the app is installed but state-fresh.

> **Reset between sections:** Settings → Apps → WizedUp Focus → Storage → Clear data, then
> uninstall + reinstall to also reset the Accessibility Service grant. Skip the reset for
> sections 3 and 4 if you're chaining them (they share active state).

---

## Section 1 — Activate in ≤ 2 taps (MISSION AC #1)

**Stories covered:** US-R1-03, US-R1-04, US-R1-05.

### Setup
- Fresh install of `app-debug.apk`.
- Accessibility Service NOT yet granted.

### Steps (first launch)
1. Tap the WizedUp Focus icon.
2. Onboarding screen appears: title "Welcome to WizedUp", a "Name or student ID" field, "Continue"
   disabled.
3. Type "Alex M.". Continue enables.
4. Tap **Continue**. Home screen appears: greeting "Hey, Alex M.", green "Focus Off"
   badge, dominant **Activate Focus** button.
5. Tap **Activate Focus**. Rationale dialog appears.
6. Tap **Open Settings**. Android Settings → Accessibility opens.
7. Find **WizedUp Focus**, toggle on, confirm the system warning.
8. Press Back twice to return to the app.
9. Tap **Activate Focus** again. (On Android 13+: allow the notification permission.)
10. Red "Focus Active" screen appears within ~500 ms.

### Expected
- Onboarding is one-time and excluded from the 2-tap metric (US-R1-04 AC).
- On the **second-and-subsequent launches**, tap-to-active is exactly **one in-app tap**
  (the "Activate Focus" button); the system Settings deep-link to grant accessibility is
  a one-time, system-side action and does not count as an in-app tap.
- Total in-app taps from cold launch to active focus: ≤ 2 in all cases.

### Emulator-vs-physical notes
- Emulator: identical behavior. Use `adb install app-debug.apk`, then launch via app
  drawer.
- Physical: same. Some OEMs intercept the Accessibility deep-link and present an
  additional "Use service?" confirmation. Tap through it.

---

## Section 2 — All other apps inaccessible (MISSION AC #2)

**Stories covered:** US-R1-06, US-R1-07, US-R1-08, US-R1-12.

### Setup
- Continue from Section 1 with focus already active (red screen visible).

### Survival matrix (run all rows; each row should pass)

| # | Action | Expected result |
|---|--------|-----------------|
| 1 | Pull down the notification shade | Shade overlays. Dismiss with swipe up — FocusActivity is back within 500 ms. |
| 2 | Press **Home** | Launcher visible for ~100–300 ms. FocusActivity reasserts. |
| 3 | Press **Recents** (app switcher) | FocusActivity is excluded from recents (per `excludeFromRecents=true`). If any other app entry is tapped, FocusActivity reasserts within 500 ms. |
| 4 | From the launcher, tap **Calculator** | Calculator opens momentarily. FocusActivity reasserts. |
| 5 | From the launcher, tap **Chrome** | Chrome opens momentarily. FocusActivity reasserts. |
| 6 | Press the power button to lock the screen, then unlock | After unlock, FocusActivity is the foreground (US-R1-12 AC, locked PM decision #4). |
| 7 | Toggle airplane mode from quick settings | No crash, no UI change. |
| 8 | `adb shell am kill com.wizedup.focus` | Notification reappears within ~5 s (START_STICKY). FocusActivity returns on next foreground change. |
| 9 | Pull notification shade and tap the "Focus Mode active" notification | FocusActivity opens immediately. |
| 10 | Press **Back** while on FocusActivity | Nothing happens. (Override per US-R1-06 AC.) |

### Emulator-vs-physical notes
- All rows are emulator-friendly on Pixel 6 API 34.
- Row 8 requires `adb`; on a physical device, use `adb` over USB.
- OEM physical devices may show a faster/slower flicker on row 4 — anything ≤ 500 ms is
  pass; > 1 s is investigate.

---

## Section 3 — Survives device reboot (MISSION AC #3)

**Stories covered:** US-R1-11.

### Setup
- Focus active (continue from Section 2 or activate again).
- Note the elapsed timer value before reboot.

### Steps
1. Reboot the device.
   - Emulator: `adb reboot` (or use the AVD's "Cold boot" menu).
   - Physical: hold power → Restart, or `adb reboot` over USB.
2. Wait for boot to complete and unlock the device.
3. **Resume UX:** The app may bring the red **Focus Active** screen forward automatically once
   the foreground service starts (boot-resume path). If it does not (OEM / timing), use the
   notification in the next step.
4. Within 10 seconds of unlock, pull the notification shade. The "Focus Mode active"
   notification should be visible.
5. If you are not already on FocusActivity, tap the notification → FocusActivity opens.
6. From the launcher, attempt to open another app (e.g. Calculator). The first attempt
   may succeed for ~5–30 s while the AccessibilityService is rebinding; subsequent
   attempts are blocked by the relaunch loop.

### Expected
- `is_active` is true after the reboot read.
- ForegroundService starts within 10 s of `BOOT_COMPLETED`.
- **Optional:** FocusActivity may appear automatically after unlock (no tap required) when
  the OS allows a start from the foreground service; otherwise the persistent notification
  remains the explicit resume path.
- Notification is ongoing and tappable.
- AccessibilityService rebinds within ~30 s and resumes the relaunch loop.
- Per locked PM decision #3, NO toast / NO banner appears — only the persistent
  notification.

### Negative case: focus inactive across reboot
1. Tap **Exit Focus** → confirm. Home screen, green state.
2. Reboot.
3. After unlock: no foreground notification, no FocusActivity. App icon is in launcher
   but no service is running.

### Emulator-vs-physical notes
- Emulator: works on Pixel 6 API 34. Cold-boot the AVD if you want a closer-to-physical
  reboot.
- Physical: required for OEM-specific killers per ADR-003. Test on at least one device
  per OEM in the pilot.

---

## Section 4 — Student can voluntarily exit at any time (MISSION AC #4)

**Stories covered:** US-R1-10.

### Setup
- Focus active (continue from Section 3 or activate fresh).

### Steps (run all four)
1. **Exit at 0 s.** Activate focus, immediately tap **Exit Focus**, confirm "Exit" — home
   screen with green badge, notification disappears.
2. **Exit at 30 s.** Activate, wait 30 seconds (verify the timer ticked to ~00:30), tap
   **Exit Focus**, tap **Stay focused** — dialog dismisses, still on focus screen.
   Tap **Exit Focus** again, tap **Exit** — home screen, green.
3. **Exit at 5 min.** Activate, wait 5 minutes (timer should read 05:00), tap **Exit**,
   confirm — home screen, green.
4. **Exit post-reboot.** Activate, reboot the device, after unlock tap the persistent
   notification, on FocusActivity tap **Exit Focus**, confirm — home screen, green,
   notification gone.

### Additional check: verbatim copy
Confirm the dialog body reads exactly: *"Your session will be logged in a future release."*
Confirm the dialog title reads exactly: *"Exit Focus Mode?"*. Per locked PM decision #2,
the copy must not be softened or dramatized.

### Edge cases
- With airplane mode on: exit still works.
- In dark mode: dialog is legible (Material3 dark theme).
- With shade pulled open: dialog stays visible because it's hosted in our activity.

### Emulator-vs-physical notes
- Identical behavior on both. Use a stopwatch / `date` for timing the longer waits.

---

## Section 5 — No school IT involvement to install (MISSION AC #5)

**Stories covered:** US-R1-03, US-R1-05.

### Setup
- A device with no MDM, no work profile, no developer-options ADB enrollment, no
  factory-reset enrollment, no QR-code provisioning.

### Steps
1. Sideload the APK (or install from Play Store internal track):
   ```bash
   adb install app-debug.apk
   ```
   On a real student device, sideload via "Install from APK" in Files app or download
   from Play Store.
2. Open the app from the launcher.
3. Onboarding: enter a name → Continue.
4. Home: tap **Activate Focus** → Open Settings → toggle WizedUp Focus on → Back.
5. Tap **Activate Focus** → grant notification permission → focus active.

### Expected
- The **only** system-level action required is granting the AccessibilityService in
  Settings (and the Notifications permission on Android 13+).
- No request to enroll the device, no work profile creation, no factory reset, no QR or
  NFC provisioning, no ADB.
- The student can install, onboard, and activate without anyone else touching the device.

### Auto Backup exclusion check (US-R1-14 DoD)
1. Install the app, complete onboarding ("Alex M.").
2. Trigger a backup: `adb shell bmgr backupnow com.wizedup.focus`.
3. Uninstall, then reinstall via `adb install -r app-debug.apk` and trigger a restore:
   `adb shell bmgr restore com.wizedup.focus`.
4. Open the app. **Expected:** onboarding screen appears (not the previous student's
   name), because `data_extraction_rules.xml` and `backup_rules.xml` exclude
   `wizedup_state.preferences_pb`.

### Emulator-vs-physical notes
- Emulator is sufficient for the install / onboarding / activation path.
- Auto Backup verification requires `adb shell bmgr` which works on emulator and
  physical alike.

---

## Sign-off checklist

When all five sections pass, QA records the following on the R1 PR:

- [ ] Section 1 passed on Pixel 6 emulator API 34.
- [ ] Section 2 (all 10 rows) passed on Pixel 6 emulator API 34.
- [ ] Section 3 passed on emulator AND on at least one physical device.
- [ ] Section 4 (all four exit-time variants + verbatim copy) passed.
- [ ] Section 5 passed AND auto-backup exclusion verified.
- [ ] No new permissions appear in `aapt dump permissions app-release.apk` beyond the six
      declared in `AndroidManifest.xml`.
- [ ] No `INTERNET`, no `SYSTEM_ALERT_WINDOW`, no `BIND_DEVICE_ADMIN`.
- [ ] Play Console accessibility-use disclosure draft is attached to the PR (per
      ADR-002 risk #1).

Once all boxes are ticked, the PM gates R1 → R2.
