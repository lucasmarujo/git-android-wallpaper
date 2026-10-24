package com.lucasmarujo.gitwallpaper.wallpaper

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import java.security.MessageDigest
import java.util.Locale

object WallpaperFingerprint {
    private const val RENDER_VERSION = 1
    private const val SEPARATOR = '|'
    private const val HASH_ALGORITHM = "SHA-256"
    private const val HEX_FORMAT = "%02x"

    fun of(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        size: ScreenSize,
        locale: Locale,
    ): String {
        val content = buildString {
            append(RENDER_VERSION).append(SEPARATOR)
            append(calendar.login).append(SEPARATOR)
            append(settings.target).append(SEPARATOR)
            append(settings.theme).append(SEPARATOR)
            append(settings.period).append(SEPARATOR)
            append(settings.cellShape).append(SEPARATOR)
            append(settings.verticalPosition).append(SEPARATOR)
            append(settings.showStats).append(SEPARATOR)
            append(size.width).append('x').append(size.height).append(SEPARATOR)
            append(locale.toLanguageTag())
            calendar.days.forEach { day ->
                append(SEPARATOR).append(day.date).append(':').append(day.count).append(':').append(day.level)
            }
        }
        return MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> HEX_FORMAT.format(byte) }
    }
}
