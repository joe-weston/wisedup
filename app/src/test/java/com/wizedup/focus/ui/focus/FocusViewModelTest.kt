package com.wizedup.focus.ui.focus

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.wizedup.focus.data.FocusStateRepository
import com.wizedup.focus.data.StudentProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FocusViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deactivate_writesInactiveThroughRepository() = runTest {
        val prefsFile = File(tempFolder.newFolder(), "focus_vm_test.preferences_pb")
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { prefsFile },
        )
        val focusRepo = FocusStateRepository(dataStore)
        val profileRepo = StudentProfileRepository(dataStore)
        profileRepo.completeOnboarding("Alex")
        focusRepo.activate(nowMs = 1L)
        assertThat(focusRepo.snapshot().isActive).isTrue()

        val vm = FocusViewModel(focusRepo, profileRepo)
        vm.deactivate()
        yield()

        assertThat(focusRepo.snapshot().isActive).isFalse()
    }
}
