package com.wizedup.focus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Placeholder instrumentation smoke test.
 *
 * We cannot run instrumentation tests in the WizedUp dev environment (no SDK / no
 * emulator), but the test is included so:
 *  - the androidTest source set is non-empty (some Gradle plugins flag empty source sets),
 *  - QA / future engineers have an obvious place to add Espresso / Compose UI tests against
 *    the onboarding flow, the home → activate → focus path, and the exit dialog.
 *
 * Intended additions (R1 follow-up if QA chooses to invest):
 *  - OnboardingFlowTest: type a name, tap Continue, assert the home screen renders.
 *  - HomeActivateTest: tap Activate Focus on a device with accessibility already granted,
 *    assert FocusActivity is the foreground.
 *  - ExitDialogTest: from FocusActivity, tap Exit Focus → "Stay focused" → still focused;
 *    Exit → home (green state) within ~500 ms.
 */
@RunWith(AndroidJUnit4::class)
class SmokeInstrumentationTest {

    @Test
    fun applicationContextIsWizedUpFocus() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.wizedup.focus", ctx.packageName)
    }
}
