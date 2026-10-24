package com.lucasmarujo.gitwallpaper.data.auth

import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.ContributionsSource
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.work.WallpaperScheduler

class AuthRepository(
    private val deviceFlowClient: DeviceFlowClient,
    private val contributionsSource: ContributionsSource,
    private val sessionRepository: SessionRepository,
    private val contributionRepository: ContributionRepository,
    private val updateStatusRepository: UpdateStatusRepository,
    private val wallpaperScheduler: WallpaperScheduler,
) {
    val isDeviceFlowAvailable: Boolean
        get() = deviceFlowClient.isConfigured

    suspend fun requestDeviceCode(): DeviceCode = deviceFlowClient.requestDeviceCode()

    suspend fun awaitDeviceAuthorization(deviceCode: DeviceCode): String =
        deviceFlowClient.awaitAccessToken(deviceCode)

    suspend fun signIn(token: String) {
        val sanitizedToken = token.trim()
        val calendar = contributionsSource.fetchCalendar(sanitizedToken)
        contributionRepository.save(calendar)
        sessionRepository.save(Session(login = calendar.login, token = sanitizedToken))
    }

    suspend fun signOut() {
        wallpaperScheduler.cancel()
        sessionRepository.clear()
        contributionRepository.clear()
        updateStatusRepository.clear()
    }
}
