# Default ProGuard rules for the WizedUp Focus app.
# R1 has minimal third-party code; most rules below cover Compose + DataStore.

# Keep our entry-point classes (registered in the manifest).
-keep class com.wizedup.focus.WizedUpApplication { *; }
-keep class com.wizedup.focus.MainActivity { *; }
-keep class com.wizedup.focus.ui.focus.FocusActivity { *; }
-keep class com.wizedup.focus.service.FocusAccessibilityService { *; }
-keep class com.wizedup.focus.service.FocusForegroundService { *; }
-keep class com.wizedup.focus.receiver.BootReceiver { *; }

# Kotlin metadata (needed for reflection-free DataStore key descriptors).
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Compose runtime relies on lambda metadata.
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# DataStore Preferences uses kotlinx-serialization-style keys; nothing to obfuscate dangerously.
-keep class androidx.datastore.preferences.** { *; }

# AndroidX lifecycle
-keep class androidx.lifecycle.** { *; }

# Ktor + kotlinx.serialization (R2 Supabase RPC)
-dontwarn io.ktor.**
-keep class kotlinx.serialization.json.** { *; }

# WorkManager (R2 outbox sync)
-keep class androidx.work.** { *; }
-keep class com.wizedup.focus.sync.OutboxSyncWorker { *; }
