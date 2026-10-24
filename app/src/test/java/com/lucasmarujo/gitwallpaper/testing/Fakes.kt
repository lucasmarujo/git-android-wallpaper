package com.lucasmarujo.gitwallpaper.testing

import android.graphics.Bitmap
import com.lucasmarujo.gitwallpaper.data.auth.SecretCipher
import com.lucasmarujo.gitwallpaper.data.github.ContributionsSource
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperApplier
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater
import com.lucasmarujo.gitwallpaper.work.WallpaperScheduler
import java.security.GeneralSecurityException
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

class FakeWallpaperUpdater(var result: UpdateResult = UpdateResult.Applied) : WallpaperUpdater {
    val forcedCalls = mutableListOf<Boolean>()

    override suspend fun update(force: Boolean): UpdateResult {
        forcedCalls += force
        return result
    }
}
