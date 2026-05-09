# Default ProGuard rules for the WisedUp Focus app.
# R1 has minimal third-party code; most rules below cover Compose + DataStore.

# Keep our entry-point classes (registered in the manifest).
-keep class com.wisedup.focus.WisedUpApplication { *; }
-keep class com.wisedup.focus.MainActivity { *; }
-keep class com.wisedup.focus.ui.focus.FocusActivity { *; }
-keep class com.wisedup.focus.service.FocusAccessibilityService { *; }
-keep class com.wisedup.focus.service.FocusForegroundService { *; }
-keep class com.wisedup.focus.receiver.BootReceiver { *; }

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

# Coroutines stack-trace recovery (helpful for crash reports if added later).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
