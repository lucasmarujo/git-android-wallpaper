package com.lucasmarujo.gitwallpaper.ui

import android.Manifest
import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ContextExtensionsTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `battery settings open the system optimization list`() {
        application.openBatteryOptimizationSettings()

        val intent = shadowOf(application).nextStartedActivity
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `privacy policy opens the published page`() {
        application.openPrivacyPolicy()

        val intent = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(PRIVACY_POLICY_URL, intent.dataString)
        assertTrue(PRIVACY_POLICY_URL.startsWith("https://"))
    }

    @Test
    fun `copied links are placed on the clipboard`() {
        application.copyToClipboard("GitHub link", "https://github.com/login/device")

        val clip = requireNotNull(application.getSystemService(ClipboardManager::class.java).primaryClip)
        assertEquals("https://github.com/login/device", clip.getItemAt(0).text)
        assertEquals("GitHub link", clip.description.label)
    }

    @Test
    fun `app does not request a direct battery optimization exemption`() {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, PackageManager.GET_PERMISSIONS)
        val requested = packageInfo.requestedPermissions.orEmpty().toList()

        assertFalse(requested.contains(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
        assertTrue(requested.contains(Manifest.permission.SET_WALLPAPER))
    }

    @Test
    fun `application id matches the published play store package`() {
        assertEquals("dev.marujo.gitwallpaper", BuildConfig.APPLICATION_ID)
    }
}
