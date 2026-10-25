package com.lucasmarujo.gitwallpaper.testing

import android.graphics.Bitmap
import com.lucasmarujo.gitwallpaper.data.auth.SecretCipher
import com.lucasmarujo.gitwallpaper.data.github.AvatarSource
import com.lucasmarujo.gitwallpaper.data.github.ContributionsSource
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperApplier
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater
import com.lucasmarujo.gitwallpaper.widget.WidgetUpdater
import com.lucasmarujo.gitwallpaper.work.WallpaperScheduler
import java.security.GeneralSecurityException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FakeSecretCipher : SecretCipher {
    var failOnDecrypt = false

    override fun encrypt(plainText: String): String =
        PREFIX + Base64.getEncoder().encodeToString(plainText.toByteArray())

    override fun decrypt(cipherText: String): String {
        if (failOnDecrypt) throw GeneralSecurityException("Key was invalidated")
        return String(Base64.getDecoder().decode(cipherText.removePrefix(PREFIX)))
    }

    private companion object {
        const val PREFIX = "enc:"
    }
}

class FakeContributionsSource(
    var handler: suspend (String) -> ContributionCalendar = { TestCalendars.calendar() },
) : ContributionsSource {
    val requestedTokens = mutableListOf<String>()

    override suspend fun fetchCalendar(token: String): ContributionCalendar {
        requestedTokens += token
        return handler(token)
    }
}

class FakeWallpaperScheduler : WallpaperScheduler {
    val scheduledIntervals: MutableList<UpdateInterval> = CopyOnWriteArrayList()
    private val cancellations = AtomicInteger()

    val cancelCount: Int
        get() = cancellations.get()

    override fun schedule(interval: UpdateInterval) {
        scheduledIntervals += interval
    }

    override fun cancel() {
        cancellations.incrementAndGet()
    }
}

class RecordingWallpaperApplier : WallpaperApplier {
    data class Applied(val width: Int, val height: Int, val target: WallpaperTarget)

    val applied = mutableListOf<Applied>()
    var failure: Exception? = null

    override fun apply(bitmap: Bitmap, target: WallpaperTarget) {
        failure?.let { throw it }
        applied += Applied(bitmap.width, bitmap.height, target)
    }
}

class FakeAvatarSource(var avatar: Bitmap? = null) : AvatarSource {
    val requestedUrls: MutableList<String?> = CopyOnWriteArrayList()

    override suspend fun load(url: String?): Bitmap? {
        requestedUrls += url
        return avatar
    }
}

class MutableClock(var instant: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant
}

class FakeWidgetUpdater(var widgetCount: Int = 0) : WidgetUpdater {
    val updatedIds: MutableList<List<Int>> = CopyOnWriteArrayList()
    private val updateAllCalls = AtomicInteger()

    val updateAllCount: Int
        get() = updateAllCalls.get()

    override fun hasWidgets(): Boolean = widgetCount > 0

    override suspend fun updateAll() {
        updateAllCalls.incrementAndGet()
    }

    override suspend fun update(appWidgetIds: IntArray) {
        updatedIds += appWidgetIds.toList()
    }
}

class FakeWallpaperUpdater(var result: UpdateResult = UpdateResult.Applied) : WallpaperUpdater {
    val forcedCalls = mutableListOf<Boolean>()

    override suspend fun update(force: Boolean): UpdateResult {
        forcedCalls += force
        return result
    }
}
