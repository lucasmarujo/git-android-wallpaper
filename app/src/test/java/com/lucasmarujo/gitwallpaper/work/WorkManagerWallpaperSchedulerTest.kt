package com.lucasmarujo.gitwallpaper.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WorkManagerWallpaperSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerWallpaperScheduler

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerWallpaperScheduler(workManager)
    }

    @Test
    fun `schedules a unique periodic update that requires network`() {
        scheduler.schedule(UpdateInterval.THREE_HOURS)

        val info = workInfos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.HOURS.toMillis(3), info.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun `rescheduling updates the existing work instead of duplicating it`() {
        scheduler.schedule(UpdateInterval.ONE_HOUR)

        scheduler.schedule(UpdateInterval.TWELVE_HOURS)

        val info = workInfos().single()
        assertEquals(TimeUnit.HOURS.toMillis(12), info.periodicityInfo?.repeatIntervalMillis)
    }

    @Test
    fun `cancel stops the periodic update`() {
        scheduler.schedule(UpdateInterval.ONE_HOUR)

        scheduler.cancel()

        assertEquals(WorkInfo.State.CANCELLED, workInfos().single().state)
    }

    private fun workInfos(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkManagerWallpaperScheduler.UNIQUE_WORK_NAME).get()
}
