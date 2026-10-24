package com.lucasmarujo.gitwallpaper.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

private const val TAG = "ContextExtensions"

fun Context.copyToClipboard(label: String, text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.openUrl(url: String) {
    startActivitySafely(Intent(Intent.ACTION_VIEW, url.toUri()))
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) ?: true

fun Context.openBatteryOptimizationSettings() {
    startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (exception: ActivityNotFoundException) {
        Log.w(TAG, "No activity can handle ${intent.action}", exception)
    }
}
