# WizedUp

WizedUp is a voluntary, accountability-first focus app for students. The repo is organized around production app surfaces plus shared backend and documentation.

## Repository Layout

```text
apps/
  android/      Android student app (Kotlin + Compose)
  web-admin/    School admin dashboard (Next.js)
  ios/          Future iOS student app (SwiftUI, R4)
docs/           ADRs, release sign-off, manual QA, provisioning
supabase/       Database migrations and backend schema
```

## Common Commands

```bash
# Android
./gradlew :android:testDebugUnitTest :android:assembleDebug --no-daemon

# Admin dashboard
cd apps/web-admin
npm install
npm run lint
npm run build
```

`local.properties` remains at the repository root for Android Supabase configuration. The Vercel project root for the dashboard is `apps/web-admin`.
