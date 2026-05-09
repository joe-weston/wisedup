package com.wisedup.focus.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unit tests for [FocusStateRepository] and [StudentProfileRepository].
 *
 * These run against a real DataStore Preferences instance backed by a temp file. We're
 * exercising the actual serialization path, not a mock, because the surface area is small
 * enough that exercising the real thing is more honest than stubbing it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusStateRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var focusRepo: FocusStateRepository
    private lateinit var profileRepo: StudentProfileRepository
    private lateinit var testScope: TestScope
    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        prefsFile = File(tempFolder.newFolder(), "wisedup_state.preferences_pb")
        testScope = TestScope(StandardTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { prefsFile },
        )
        focusRepo = FocusStateRepository(dataStore)
        profileRepo = StudentProfileRepository(dataStore)
    }

    @After
    fun tearDown() {
        try {
            testScope.cancel(CancellationException("test done"))
        } catch (_: Throwable) {
            // ignore
        }
    }

    // -- focus.* --

    @Test
    fun `default state is inactive on a fresh store`() = runTest {
        focusRepo.state.test {
            val first = awaitItem()
            assertThat(first.isActive).isFalse()
            assertThat(first.startedAtMs).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activate sets is_active true and started_at_ms now`() = runTest {
        val before = System.currentTimeMillis()
        focusRepo.activate()
        val after = System.currentTimeMillis()

        focusRepo.state.test {
            val s = awaitItem()
            assertThat(s.isActive).isTrue()
            assertThat(s.startedAtMs).isNotNull()
            // Allow ±1 s skew for test timing on slow CI.
            assertThat(s.startedAtMs!! >= before - 1_000L).isTrue()
            assertThat(s.startedAtMs!! <= after + 1_000L).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deactivate clears flag and started_at`() = runTest {
        focusRepo.activate()
        focusRepo.deactivate()

        focusRepo.state.test {
            val s = awaitItem()
            assertThat(s.isActive).isFalse()
            assertThat(s.startedAtMs).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snapshot returns same value as flow first emission`() = runTest {
        focusRepo.activate()

        // snapshot uses runBlocking under the hood; we exercise it but accept that it
        // bridges out of the test scheduler. With a real DataStore the value is already
        // committed by activate() so the read is fast.
        val snap = focusRepo.snapshot(timeoutMs = 5_000L)
        assertThat(snap.isActive).isTrue()
        assertThat(snap.startedAtMs).isNotNull()
    }

    // -- student.* --

    @Test
    fun `profile is null on a fresh store`() = runTest {
        profileRepo.profile.test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeOnboarding writes UUID id and trimmed name`() = runTest {
        val before = System.currentTimeMillis()
        val profile = profileRepo.completeOnboarding("  Alex M.  ")
        val after = System.currentTimeMillis()

        assertThat(profile.displayName).isEqualTo("Alex M.")
        // UUID parses without throwing.
        UUID.fromString(profile.id)
        assertThat(profile.createdAtMs >= before).isTrue()
        assertThat(profile.createdAtMs <= after).isTrue()

        profileRepo.profile.test {
            val emitted = awaitItem()
            assertThat(emitted).isNotNull()
            assertThat(emitted!!.displayName).isEqualTo("Alex M.")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completeOnboarding throws on empty input`() = runTest {
        profileRepo.completeOnboarding("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completeOnboarding throws on whitespace-only input`() = runTest {
        profileRepo.completeOnboarding("    ")
    }

    @Test
    fun `completeOnboarding accepts exactly 64 chars`() = runTest {
        val sixtyFour = "a".repeat(64)
        val profile = profileRepo.completeOnboarding(sixtyFour)
        assertThat(profile.displayName).isEqualTo(sixtyFour)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completeOnboarding throws on 65 chars`() = runTest {
        profileRepo.completeOnboarding("a".repeat(65))
    }

    @Test
    fun `completeOnboarding does not write when validation fails`() = runTest {
        try {
            profileRepo.completeOnboarding("")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        profileRepo.profile.test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeated onboarding preserves existing UUID`() = runTest {
        val first = profileRepo.completeOnboarding("Alex")
        val second = profileRepo.completeOnboarding("Alex M.")
        assertThat(second.id).isEqualTo(first.id)
        assertThat(second.displayName).isEqualTo("Alex M.")
    }
}
