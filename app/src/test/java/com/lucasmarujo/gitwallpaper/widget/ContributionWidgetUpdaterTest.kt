package com.lucasmarujo.gitwallpaper.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.MainActivity
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.testing.FakeContributionsSource
import com.lucasmarujo.gitwallpaper.testing.FakeSecretCipher
import com.lucasmarujo.gitwallpaper.testing.TestCalendars
import com.lucasmarujo.gitwallpaper.testing.preferencesDataStore
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionWallpaperRenderer
import com.lucasmarujo.gitwallpaper.wallpaper.ScreenSize
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAppWidgetManager
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContributionWidgetUpdaterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val appWidgetManager = AppWidgetManager.getInstance(application)
    private val shadowManager: ShadowAppWidgetManager = shadowOf(appWidgetManager)
    private val provider = ComponentName(application, ContributionWidgetProvider::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC)
    private val renderer = ContributionWallpaperRenderer(application.resources)
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var settingsRepository: SettingsRepository
    private var nextWidgetId = 1

    @Before
    fun allowBinding() {
        shadowManager.setAllowedToBindAppWidgets(true)
    }

    @Test
    fun `each widget shows the cached graph at its own size`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        val wide = bindWidget(widthDp = 250, heightDp = 110)
        val tall = bindWidget(widthDp = 110, heightDp = 250)

        updater.updateAll()

        assertEquals(250 to 110, widgetBitmap(wide).let { it.width to it.height })
        assertEquals(110 to 250, widgetBitmap(tall).let { it.width to it.height })
    }

    @Test
    fun `widgets follow the current settings`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        val settings = settingsRepository.update { WallpaperSettings(theme = WallpaperTheme.OCEAN, showStats = false) }
        val widget = bindWidget(widthDp = 250, heightDp = 110)

        updater.update(intArrayOf(widget))

        val expected = renderer.renderWidget(
            TestCalendars.calendar(),
            settings,
            ScreenSize(250, 110),
            application.resources.displayMetrics.density,
            LocalDate.now(clock),
        )
        assertTrue(expected.sameAs(widgetBitmap(widget)))
    }

    @Test
    fun `without contributions the widget asks to open the app`() = runTest {
        val updater = updater()
        val widget = bindWidget(widthDp = 250, heightDp = 110)

        updater.updateAll()

        val message = shadowManager.getViewFor(widget).findViewById<TextView>(R.id.widget_message)
        assertEquals(application.getString(R.string.widget_empty), message.text.toString())
    }

    @Test
    fun `tapping the widget opens the app`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        val widget = bindWidget(widthDp = 250, heightDp = 110)
        updater.updateAll()

        shadowManager.getViewFor(widget).findViewById<View>(android.R.id.background).performClick()

        val started = shadowOf(application).nextStartedActivity
        assertEquals(MainActivity::class.java.name, started.component?.className)
    }

    @Test
    fun `huge widgets are drawn with a bounded bitmap that keeps the aspect ratio`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        val widget = bindWidget(widthDp = 2000, heightDp = 1000)

        updater.updateAll()

        val bitmap = widgetBitmap(widget)
        assertTrue(bitmap.width * bitmap.height <= MAX_BITMAP_PIXELS)
        assertEquals(2f, bitmap.width / bitmap.height.toFloat(), 0.01f)
    }

    @Test
    fun `widgets without size information use the default size`() = runTest {
        val updater = updater()
        contributionRepository.save(TestCalendars.calendar())
        val widget = nextWidgetId++
        appWidgetManager.bindAppWidgetIdIfAllowed(widget, provider)

        updater.updateAll()

        assertEquals(250 to 80, widgetBitmap(widget).let { it.width to it.height })
    }

    @Test
    fun `has widgets reflects the widgets placed on the home screen`() = runTest {
        val updater = updater()

        assertFalse(updater.hasWidgets())
        bindWidget(widthDp = 250, heightDp = 110)
        assertTrue(updater.hasWidgets())
    }

    private fun bindWidget(widthDp: Int, heightDp: Int): Int {
        val appWidgetId = nextWidgetId++
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        assertTrue(appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider, options))
        return appWidgetId
    }

    private fun widgetBitmap(appWidgetId: Int): Bitmap {
        val image = shadowManager.getViewFor(appWidgetId).findViewById<ImageView>(R.id.widget_image)
        return (image.drawable as BitmapDrawable).bitmap
    }

    private fun TestScope.updater(): ContributionWidgetUpdater {
        val sessionRepository = SessionRepository(temporaryFolder.preferencesDataStore(backgroundScope), FakeSecretCipher())
        contributionRepository = ContributionRepository(
            temporaryFolder.preferencesDataStore(backgroundScope),
            FakeContributionsSource(),
            sessionRepository,
        )
        settingsRepository = SettingsRepository(temporaryFolder.preferencesDataStore(backgroundScope))
        return ContributionWidgetUpdater(
            context = application,
            appWidgetManager = appWidgetManager,
            contributionRepository = contributionRepository,
            settingsRepository = settingsRepository,
            renderer = renderer,
            clock = clock,
            renderDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    private companion object {
        const val MAX_BITMAP_PIXELS = 1_000_000
    }
}
