package com.wizedup.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.wizedup.focus.compliance.ComplianceBypassReporter
import com.wizedup.focus.compliance.ComplianceCoordinator
import com.wizedup.focus.data.FocusStateRepository
import com.wizedup.focus.data.SchoolRegistrationRepository
import com.wizedup.focus.data.StudentProfileRepository
import com.wizedup.focus.data.remote.SupabaseRestClient
import com.wizedup.focus.data.sync.OutboxRepository
import com.wizedup.focus.data.sync.SyncWorkScheduler
import com.wizedup.focus.data.sync.WizedUpDatabase
import com.wizedup.focus.data.wizedupDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Manual DI per the engineering brief — Hilt is intentionally NOT used in R1 to keep the
 * surface area small. R2 adds Room + WorkManager + Supabase RPC wiring without a DI
 * framework; see ADR-005.
 */
class WizedUpApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var focusStateRepository: FocusStateRepository
        private set

    lateinit var studentProfileRepository: StudentProfileRepository
        private set

    lateinit var schoolRegistrationRepository: SchoolRegistrationRepository
        private set

    lateinit var outboxRepository: OutboxRepository
        private set

    var supabaseRestClient: SupabaseRestClient? = null
        private set

    private lateinit var roomDatabase: WizedUpDatabase
    private lateinit var complianceCoordinator: ComplianceCoordinator

    override fun onCreate() {
        super.onCreate()
        instance = this

        focusStateRepository = FocusStateRepository(wizedupDataStore)
        studentProfileRepository = StudentProfileRepository(wizedupDataStore)

        roomDatabase = Room.databaseBuilder(this, WizedUpDatabase::class.java, "wizedup_room.db")
            .fallbackToDestructiveMigration()
            .build()
        outboxRepository = OutboxRepository(this, roomDatabase.pendingSyncDao())
        supabaseRestClient = SupabaseRestClient.fromBuildConfig()
        schoolRegistrationRepository = SchoolRegistrationRepository(
            appContext = this,
            dataStore = wizedupDataStore,
            supabase = supabaseRestClient,
        )

        complianceCoordinator = ComplianceCoordinator(
            scope = applicationScope,
            focusRepo = focusStateRepository,
            schoolRepo = schoolRegistrationRepository,
            outbox = outboxRepository,
            backendConfigured = { supabaseRestClient != null },
        )
        complianceCoordinator.start()

        createNotificationChannel()
    }

    /** Called when the accessibility service unbinds while focus may still be active (ADR-002 / R2). */
    fun enqueueAccessibilityBypassIfNeeded() {
        applicationScope.launch {
            ComplianceBypassReporter.reportAccessibilityDisabled(
                focusRepo = focusStateRepository,
                schoolRepo = schoolRegistrationRepository,
                outbox = outboxRepository,
                backendConfigured = supabaseRestClient != null,
            )
        }
    }

    /** Best-effort: drain any pending compliance rows after reboot (R2 offline queue). */
    fun scheduleComplianceSyncIfNeeded() {
        applicationScope.launch {
            if (supabaseRestClient != null && schoolRegistrationRepository.isRegisteredSnapshot()) {
                SyncWorkScheduler.schedule(this@WizedUpApplication)
            }
        }
    }

    /**
     * Creates the high-importance notification channel used by [FocusForegroundService].
     * Idempotent — calling [NotificationManager.createNotificationChannel] with the same
     * channel ID is a no-op after the first call.
     *
     * Per US-R1-09 ACs. The architecture spec calls for IMPORTANCE_HIGH; the engineering
     * brief asks for IMPORTANCE_LOW. We honor the engineering brief: IMPORTANCE_LOW makes
     * the notification non-intrusive (no heads-up, no sound) which is correct for an
     * always-on status indicator. The channel name and description match US-R1-09.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID_FOCUS_MODE,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_FOCUS_MODE = "focus_mode"

        @Volatile
        private var instance: WizedUpApplication? = null

        /**
         * Used by services and receivers that aren't part of the Compose graph.
         * Will throw if called before [onCreate]; that should never happen in practice.
         */
        fun get(): WizedUpApplication =
            instance ?: error("WizedUpApplication.onCreate has not run yet")
    }
}
