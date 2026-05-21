# WizedUp Focus — Android Brick (R1)

The voluntary, accountability-first focus app for students. R1 is the Android-only "brick":
on-device only, no network, no logging. See `../MISSION.md` for project-wide context and
`../docs/architecture/R1_architecture.md` for the architecture this module implements.

## What this app does

1. Asks for the student's name once on first launch.
2. Lets the student tap **Activate Focus** to lock the device to a single red "Focus
   Active" screen.
3. Holds a foreground notification ("Focus Mode active — tap to return").
4. Detects any other app coming to the foreground and snaps back to the focus screen.
5. Survives screen lock, notification pull-down, app switcher, and reboot.
6. Lets the student tap **Exit Focus** at any time (with a single confirmation) to
   return to the green "Focus Off" home screen.

## Project layout

```
android/
  settings.gradle.kts
  build.gradle.kts            // root
  gradle/
    libs.versions.toml        // version catalog
    wrapper/gradle-wrapper.properties  // Gradle 8.7
  app/
    build.gradle.kts          // applicationId com.wizedup.focus
    proguard-rules.pro
    src/main/
      AndroidManifest.xml
      java/com/wizedup/focus/
        WizedUpApplication.kt
        MainActivity.kt
        ui/
          onboarding/         // OnboardingScreen + ViewModel
          home/               // HomeScreen + ViewModel
          focus/              // FocusActivity + Screen + ViewModel
          permissions/        // AccessibilityPermissionScreen (alt full-screen)
          theme/              // Compose theme + tokens
        service/
          FocusForegroundService.kt        // type=specialUse
          FocusAccessibilityService.kt     // relaunch loop
          FocusServiceController.kt        // start/stop helpers
        receiver/
          BootReceiver.kt
        data/
          FocusStateRepository.kt
          PreferencesKeys.kt
        util/
          AccessibilityUtils.kt
          SystemUiUtils.kt
      res/
        xml/{accessibility_service_config,backup_rules,data_extraction_rules}.xml
        values/{strings,colors,themes}.xml
        drawable/             // launcher + notification icon
        mipmap-anydpi-v26/    // adaptive launcher icons
    src/test/                 // unit tests (DataStore, FocusServiceController)
    src/androidTest/          // smoke instrumentation placeholder
  docs/
    MANUAL_TEST_PLAN.md       // 5 sections, one per MISSION.md AC
```

## Open in Android Studio

1. **Android Studio Hedgehog** (2023.1) or newer is required for AGP 8.5.x and Kotlin
   1.9.24.
2. Open the `android/` directory directly (not the repo root).
3. On first sync, accept the prompt to install Compose Compiler 1.5.14, AGP 8.5.2,
   Build-Tools 34, and Platform 34 if Studio offers them.
4. Build → **Make Project**. The first build will download the Gradle 8.7 distribution.

## How to grant the accessibility permission

Focus Mode requires the student to enable the Accessibility Service once.

1. Launch the app, complete onboarding (enter your name → Continue).
2. On the home screen, tap **Activate Focus**.
3. A dialog explains why we need Accessibility. Tap **Open Settings**.
4. In Settings → Accessibility → Installed Services → **WizedUp Focus**, toggle
   the service **on** and confirm.
5. Press Back to return to the app and tap **Activate Focus** again.
6. On Android 13+, allow the Notifications permission when prompted.

You'll only be asked to do this once per install. After that, **Activate Focus** flips
the lock on directly.

## Running tests

Unit tests:
```bash
./gradlew :app:test
```

Instrumentation tests (requires connected device or emulator):
```bash
./gradlew :app:connectedDebugAndroidTest
```

The instrumentation test in this branch is a smoke placeholder; QA may extend it.

## Building a release APK

```bash
./gradlew :app:assembleRelease
```

R8 / shrinking is enabled in release. No signing config is checked in; CI / release
infra should inject one.

## Manual testing

See `docs/MANUAL_TEST_PLAN.md`. It walks through each MISSION.md acceptance criterion
on a Pixel 6 emulator (API 34) and a physical device.

## Known limitations and disclosures

### 1. Google Play Store accessibility-use review

WizedUp Focus uses the `AccessibilityService` API to detect when a non-focus app is
foregrounded and to bring the focus screen back. This is **not** a traditional
accessibility use case (assisting users with disabilities). Google Play reviews any app
using `BIND_ACCESSIBILITY_SERVICE` for non-accessibility purposes and may reject or
restrict distribution. Mitigations:

- The service description and listing must clearly disclose the focus-enforcement use.
- A side-load APK must be available for pilot schools as a fallback.
- See ADR-002 §"Negative" and the architect risk register (#1) for the full discussion.

### 2. Boot → accessibility rebind gap (5–30 s)

After a reboot, the foreground service starts within ~10 s and posts the persistent
notification. The Accessibility Service rebinds on its own schedule, typically
5–30 seconds. During that window a determined student could open another app; the lock
screen reasserts the moment the Accessibility Service binds. The notification is the
visible bridge. **This is documented behavior, not a defect.** See ADR-003.

### 3. Force-stop from Settings is unrecoverable until next launch

If a student goes to Settings → Apps → WizedUp Focus → **Force stop**, the foreground
service and Accessibility Service are killed and our process can't bring itself back.
The student must launch the app again. R2 will log this absence as a `bypass_attempted`
event.

### 4. OEM aggressive-killer behavior

Xiaomi (MIUI), Huawei (EMUI), OnePlus (OxygenOS), and Samsung's "Deep Sleep" can kill
even foreground services on some configurations. The architecture mitigates this with
a foreground notification + Accessibility Service binding, but field-testing on at
least one device per OEM is required before pilot. See ADR-003.

### 5. R1 has no Hilt / no DI framework

For R1 we wire dependencies manually: `WizedUpApplication` holds two `lateinit`
repositories and any non-Composable component (services, receivers) calls
`WizedUpApplication.get()`. Hilt is a possible R2 refactor when the network/sync layer
makes the graph non-trivial.

## Acceptance-criteria traceability

The five MISSION.md R1 acceptance criteria map to user stories like this:

| MISSION AC | Primary stories | Files |
|---|---|---|
| Activate in ≤ 2 taps | US-R1-04, US-R1-05 | `HomeScreen.kt`, `HomeViewModel.kt`, `MainActivity.kt` |
| All other apps inaccessible | US-R1-08 | `FocusAccessibilityService.kt`, `accessibility_service_config.xml`, `FocusActivity.kt` |
| Survives reboot | US-R1-11 | `BootReceiver.kt`, `FocusForegroundService.kt`, `FocusStateRepository.snapshot()` |
| Voluntary exit | US-R1-10 | `FocusScreen.kt` (ExitConfirmDialog), `FocusViewModel.deactivate()` |
| No school IT involvement | US-R1-03, US-R1-05 | `OnboardingScreen.kt`, deep-link to `Settings.ACTION_ACCESSIBILITY_SETTINGS` |
