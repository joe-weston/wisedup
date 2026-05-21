# WizedUp Android

Native Android student app for R1/R2. The app ID and package remain `com.wizedup.focus`.

Run from the repository root:

```bash
./gradlew :android:testDebugUnitTest :android:assembleDebug --no-daemon
```

Supabase values are read from root `local.properties`:

```properties
SUPABASE_URL=...
SUPABASE_PUBLISHABLE_KEY=...
```
