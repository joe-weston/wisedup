package com.wizedup.focus.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.wizedup.focus.service.FocusAccessibilityService

/**
 * Helpers for inspecting accessibility-service state.
 *
 * Android does NOT expose a typed query for "is my service enabled"; we have to parse the
 * colon-separated list in [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES] and look for our
 * fully-qualified [ComponentName]. This is the standard idiom — see the Android Accessibility
 * Sample.
 */
object AccessibilityUtils {

    /**
     * @return true if [FocusAccessibilityService] is currently enabled in the system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, FocusAccessibilityService::class.java)
        val expected = expectedComponent.flattenToString()

        val enabledSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        if (enabledSetting.isEmpty()) return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledSetting) }
        for (component in splitter) {
            // Match either flattened-to-string or short forms; some OEMs serialize differently.
            if (component.equals(expected, ignoreCase = true)) return true
            // Some OEMs flatten with shortClassName; compare against that too.
            if (ComponentName.unflattenFromString(component) == expectedComponent) return true
        }
        return false
    }
}
