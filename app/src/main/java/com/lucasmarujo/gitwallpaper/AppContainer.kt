package com.lucasmarujo.gitwallpaper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.lucasmarujo.gitwallpaper.data.auth.AuthRepository
import com.lucasmarujo.gitwallpaper.data.auth.DeviceFlowClient
import com.lucasmarujo.gitwallpaper.data.auth.KeystoreSecretCipher
import com.lucasmarujo.gitwallpaper.data.auth.SessionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.github.AvatarSource
import com.lucasmarujo.gitwallpaper.data.github.CachedAvatarRepository
import com.lucasmarujo.gitwallpaper.data.github.GitHubGraphQlClient
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionWallpaperRenderer
import com.lucasmarujo.gitwallpaper.wallpaper.DefaultWallpaperUpdater
import com.lucasmarujo.gitwallpaper.wallpaper.DisplayScreenSizeProvider
import com.lucasmarujo.gitwallpaper.wallpaper.ScreenSizeProvider
import com.lucasmarujo.gitwallpaper.wallpaper.SystemWallpaperApplier
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater
import com.lucasmarujo.gitwallpaper.work.WallpaperScheduler
import com.lucasmarujo.gitwallpaper.work.WorkManagerWallpaperScheduler
import okhttp3.OkHttpClient
import java.io.File
import java.time.Clock
import java.util.concurrent.TimeUnit

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.contributionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "contributions")
private val Context.updateStatusDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_status")

class AppContainer(private val context: Context) {

    val clock: Clock = Clock.systemDefaultZone()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header(USER_AGENT_HEADER, USER_AGENT).build())
            }
            .build()
    }

    private val gitHubClient: GitHubGraphQlClient by lazy { GitHubGraphQlClient(httpClient) }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(context.sessionDataStore, KeystoreSecretCipher())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context.settingsDataStore) }

    val updateStatusRepository: UpdateStatusRepository by lazy {
        UpdateStatusRepository(context.updateStatusDataStore)
    }

    val contributionRepository: ContributionRepository by lazy {
        ContributionRepository(context.contributionsDataStore, gitHubClient, sessionRepository)
    }

    val wallpaperScheduler: WallpaperScheduler by lazy {
        WorkManagerWallpaperScheduler(WorkManager.getInstance(context))
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            deviceFlowClient = DeviceFlowClient(httpClient, BuildConfig.GITHUB_CLIENT_ID),
            contributionsSource = gitHubClient,
            sessionRepository = sessionRepository,
            contributionRepository = contributionRepository,
            updateStatusRepository = updateStatusRepository,
            wallpaperScheduler = wallpaperScheduler,
        )
    }

    val wallpaperRenderer: ContributionWallpaperRenderer by lazy { ContributionWallpaperRenderer(context.resources) }

    val screenSizeProvider: ScreenSizeProvider by lazy { DisplayScreenSizeProvider(context) }

    val avatarSource: AvatarSource by lazy {
        CachedAvatarRepository(httpClient, File(context.noBackupFilesDir, AVATAR_CACHE_DIRECTORY))
    }

    val wallpaperUpdater: WallpaperUpdater by lazy {
        DefaultWallpaperUpdater(
            contributionRepository = contributionRepository,
            settingsRepository = settingsRepository,
            updateStatusRepository = updateStatusRepository,
            avatarSource = avatarSource,
            renderer = wallpaperRenderer,
            applier = SystemWallpaperApplier(context),
            screenSizeProvider = screenSizeProvider,
            clock = clock,
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L
        const val CALL_TIMEOUT_SECONDS = 45L
        const val USER_AGENT_HEADER = "User-Agent"
        const val USER_AGENT = "GitWallpaper-Android/${BuildConfig.VERSION_NAME}"
        const val AVATAR_CACHE_DIRECTORY = "avatar"
    }
}
