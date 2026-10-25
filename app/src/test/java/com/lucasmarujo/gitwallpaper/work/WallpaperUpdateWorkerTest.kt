package com.lucasmarujo.gitwallpaper.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.lucasmarujo.gitwallpaper.GitWallpaperApplication
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperScheduler
import com.lucasmarujo.gitwallpaper.testing.FakeWallpaperUpdater
import com.lucasmarujo.gitwallpaper.testing.FakeWidgetUpdater
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperUpdateWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val updater = FakeWallpaperUpdater()
    private val scheduler = FakeWallpaperScheduler()
    private val widgetUpdater = FakeWidgetUpdater()

    @Test
    fun `applied and unchanged updates succeed without forcing`() = runTest {
        listOf(UpdateResult.Applied, UpdateResult.Unchanged).forEach { result ->
            updater.result = result

            assertEquals(ListenableWorker.Result.success(), worker().doWork())
        }
        assertEquals(listOf(false, false), updater.forcedCalls)
    }

    @Test
    fun `network and github failures are retried`() = runTest {
        listOf(SyncError.NETWORK, SyncError.GITHUB).forEach { error ->
            updater.result = UpdateResult.Failure(error)

            assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 0).doWork())
        }
    }

    @Test
    fun `retries stop after the maximum number of attempts`() = runTest {
        updater.result = UpdateResult.Failure(SyncError.NETWORK)

        assertEquals(ListenableWorker.Result.failure(), worker(runAttemptCount = 3).doWork())
    }

    @Test
    fun `lost sessions cancel the periodic updates`() = runTest {
        listOf(SyncError.SESSION_EXPIRED, SyncError.NOT_SIGNED_IN).forEach { error ->
            updater.result = UpdateResult.Failure(error)

            assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        }
        assertEquals(2, scheduler.cancelCount)
    }

    @Test
    fun `wallpaper failures fail the run but keep the schedule`() = runTest {
        updater.result = UpdateResult.Failure(SyncError.WALLPAPER)

        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `every run redraws the home screen widgets`() = runTest {
        val results = listOf(
            UpdateResult.Applied,
            UpdateResult.Unchanged,
            UpdateResult.Failure(SyncError.NETWORK),
            UpdateResult.Failure(SyncError.NOT_SIGNED_IN),
            UpdateResult.Failure(SyncError.WALLPAPER),
        )
        results.forEach { result ->
            updater.result = result

            worker().doWork()
        }

        assertEquals(results.size, widgetUpdater.updateAllCount)
    }

    @Test
    fun `application factory creates the update worker`() {
        val application = ApplicationProvider.getApplicationContext<GitWallpaperApplication>()
        val factory = application.workManagerConfiguration.workerFactory

        val worker = TestListenableWorkerBuilder<WallpaperUpdateWorker>(context)
            .setWorkerFactory(factory)
            .build()

        assertTrue(worker is WallpaperUpdateWorker)
    }

    @Test
    fun `application factory leaves other workers to the default factory`() {
        val application = ApplicationProvider.getApplicationContext<GitWallpaperApplication>()

        val worker = TestListenableWorkerBuilder<NoOpWorker>(context)
            .setWorkerFactory(WallpaperWorkerFactory(application.container))
            .build()

        assertTrue(worker is NoOpWorker)
    }

    class NoOpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }

    private fun worker(runAttemptCount: Int = 0): WallpaperUpdateWorker =
        TestListenableWorkerBuilder<WallpaperUpdateWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = WallpaperUpdateWorker(appContext, workerParameters, updater, scheduler, widgetUpdater)
                },
            )
            .build()
}
