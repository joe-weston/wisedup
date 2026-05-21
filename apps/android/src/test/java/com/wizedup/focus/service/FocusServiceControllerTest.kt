package com.wizedup.focus.service

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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
        assertThat(
            intent.getBooleanExtra(FocusForegroundService.EXTRA_FROM_BOOT_RESUME, false),
        ).isFalse()
    }

    @Test
    fun `startIntent with bootResume sets extra`() {
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.packageName } returns "com.wizedup.focus.test"

        val intent = FocusServiceController.startIntent(ctx, fromBootResume = true)

        assertThat(
            intent.getBooleanExtra(FocusForegroundService.EXTRA_FROM_BOOT_RESUME, false),
        ).isTrue()
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
    fun `stop calls stopService`() {
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.packageName } returns "com.wizedup.focus.test"

        FocusServiceController.stop(ctx)

        verify(exactly = 1) {
            ctx.stopService(
                match {
                    it.component?.className == FocusForegroundService::class.java.name
                },
            )
        }
    }
}
