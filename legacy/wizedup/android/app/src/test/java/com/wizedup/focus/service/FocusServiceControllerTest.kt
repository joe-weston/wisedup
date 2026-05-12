package com.wizedup.focus.service

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

/**
 * Verifies the start/stop intents produced by [FocusServiceController] are well-formed.
 *
 * We don't try to assert ContextCompat.startForegroundService here — that path branches on
 * the runtime SDK level and isn't worth a Robolectric round-trip. Instead we verify the
 * stop path uses the expected service class and action, and that the start intent carries
 * our [FocusForegroundService.ACTION_START] action.
 */
class FocusServiceControllerTest {

    @Test
    fun `startIntent targets FocusForegroundService with ACTION_START`() {
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.packageName } returns "com.wizedup.focus.test"

        val intent = FocusServiceController.startIntent(ctx)

        assertThat(intent.component?.className).isEqualTo(
            FocusForegroundService::class.java.name,
        )
        assertThat(intent.action).isEqualTo(FocusForegroundService.ACTION_START)
    }

    @Test
    fun `stopIntent targets FocusForegroundService with ACTION_STOP`() {
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.packageName } returns "com.wizedup.focus.test"

        val intent = FocusServiceController.stopIntent(ctx)

        assertThat(intent.component?.className).isEqualTo(
            FocusForegroundService::class.java.name,
        )
        assertThat(intent.action).isEqualTo(FocusForegroundService.ACTION_STOP)
    }

    @Test
    fun `stop sends the stop intent and then stopService`() {
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.packageName } returns "com.wizedup.focus.test"

        val captured = slot<android.content.Intent>()
        every { ctx.startService(capture(captured)) } returns null

        FocusServiceController.stop(ctx)

        verify(exactly = 1) { ctx.startService(any()) }
        verify(exactly = 1) { ctx.stopService(any()) }
        assertThat(captured.captured.action).isEqualTo(FocusForegroundService.ACTION_STOP)
    }
}
