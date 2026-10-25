package com.lucasmarujo.gitwallpaper.ui

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.lucasmarujo.gitwallpaper.widget.ContributionWidgetProvider

private const val TAG = "ContextExtensions"
const val PRIVACY_POLICY_URL = "https://lucasmarujo.github.io/git-android-wallpaper/privacy/"

fun Context.copyToClipboard(label: String, text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.openUrl(url: String) {
    startActivitySafely(Intent(Intent.ACTION_VIEW, url.toUri()))
}

fun Context.openPrivacyPolicy() {
    openUrl(PRIVACY_POLICY_URL)
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) ?: true

fun Context.openBatteryOptimizationSettings() {
    startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

fun Context.canPinContributionWidget(): Boolean =
    getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true

fun Context.requestPinContributionWidget(): Boolean {
    val appWidgetManager = getSystemService(AppWidgetManager::class.java) ?: return false
    return try {
        appWidgetManager.requestPinAppWidget(ComponentName(this, ContributionWidgetProvider::class.java), null, null)
    } catch (exception: IllegalStateException) {
        Log.w(TAG, "The launcher refused to pin the widget", exception)
        false
    }
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (exception: ActivityNotFoundException) {
        Log.w(TAG, "No activity can handle ${intent.action}", exception)
    }
}
