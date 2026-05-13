package com.wizedup.focus.ui.home

import com.google.common.truth.Truth.assertThat
import com.wizedup.focus.data.FocusStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `activate persists focus and invokes callback`() = runTest {
        val repo = mockk<FocusStateRepository>(relaxed = true)
        coEvery { repo.activate() } coAnswers { }

        val vm = HomeViewModel(repo)
        var callbackInvoked = false
        vm.activate { callbackInvoked = true }

        advanceUntilIdle()

        coVerify(exactly = 1) { repo.activate() }
        assertThat(callbackInvoked).isTrue()
    }
}
