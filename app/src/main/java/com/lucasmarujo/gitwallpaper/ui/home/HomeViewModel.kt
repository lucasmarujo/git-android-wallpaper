package com.lucasmarujo.gitwallpaper.ui.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucasmarujo.gitwallpaper.AppContainer
import com.lucasmarujo.gitwallpaper.data.auth.AuthRepository
import com.lucasmarujo.gitwallpaper.data.contributions.ContributionRepository
import com.lucasmarujo.gitwallpaper.data.contributions.RefreshResult
import com.lucasmarujo.gitwallpaper.data.github.AvatarSource
import com.lucasmarujo.gitwallpaper.data.settings.SettingsRepository
import com.lucasmarujo.gitwallpaper.data.settings.UpdateStatusRepository
import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.SyncError
import com.lucasmarujo.gitwallpaper.domain.UpdateResult
import com.lucasmarujo.gitwallpaper.domain.WallpaperFraming
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.wallpaper.ContributionWallpaperRenderer
import com.lucasmarujo.gitwallpaper.wallpaper.DayGrid
import com.lucasmarujo.gitwallpaper.wallpaper.ScreenSize
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperUpdater
import com.lucasmarujo.gitwallpaper.work.WallpaperScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

class HomeViewModel(
    private val login: String,
    private val contributionRepository: ContributionRepository,
    private val settingsRepository: SettingsRepository,
    private val updateStatusRepository: UpdateStatusRepository,
    private val wallpaperUpdater: WallpaperUpdater,
    private val wallpaperScheduler: WallpaperScheduler,
    private val authRepository: AuthRepository,
    private val avatarSource: AvatarSource,
    private val renderer: ContributionWallpaperRenderer,
    private val previewSize: ScreenSize,
    private val adjustmentSize: ScreenSize,
    private val clock: Clock,
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private data class ActionState(
        val isAdjusting: Boolean = false,
        val isApplying: Boolean = false,
        val isRefreshing: Boolean = false,
        val initialRefreshFinished: Boolean = false,
    )

    private data class RenderedPreview(val image: ImageBitmap, val stats: ContributionStats)

    private val actionState = MutableStateFlow(ActionState())
    private val _messages = Channel<HomeMessage>(Channel.BUFFERED)
    val messages: Flow<HomeMessage> = _messages.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val preview: Flow<RenderedPreview?> =
        combine(contributionRepository.calendar, settingsRepository.settings) { calendar, settings ->
            calendar to settings
        }.mapLatest { (calendar, settings) ->
            calendar?.let {
                val avatar = avatarSource.load(it.avatarUrl)
                withContext(renderDispatcher) { renderPreview(it, settings, avatar) }
            }
        }.onStart { emit(null) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val adjustmentPreview: Flow<ImageBitmap?> = combine(
        contributionRepository.calendar,
        settingsRepository.settings.map { it.copy(framing = WallpaperFraming()) },
        actionState.map { it.isAdjusting },
    ) { calendar, settings, isAdjusting ->
        calendar?.takeIf { isAdjusting }?.let { it to settings }
    }.distinctUntilChanged().mapLatest { input ->
        input?.let { (calendar, settings) ->
            val avatar = avatarSource.load(calendar.avatarUrl)
            withContext(renderDispatcher) {
                renderer.render(calendar, settings, adjustmentSize, LocalDate.now(clock), avatar).asImageBitmap()
            }
        }
    }.onStart { emit(null) }

    private val actionStateWithAdjustment: Flow<Pair<ActionState, ImageBitmap?>> =
        combine(actionState, adjustmentPreview, ::Pair)

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        contributionRepository.calendar,
        preview,
        updateStatusRepository.status,
        actionStateWithAdjustment,
    ) { settings, calendar, renderedPreview, status, (actions, adjustment) ->
        HomeUiState(
            login = login,
            settings = settings,
            preview = renderedPreview?.image,
            previewAspectRatio = previewSize.width.toFloat() / previewSize.height,
            stats = renderedPreview?.stats,
            hasCalendar = calendar != null,
            isLoadingCalendar = calendar == null && (actions.isRefreshing || !actions.initialRefreshFinished),
            lastAppliedAtMillis = status.lastAppliedAtMillis,
            isAdjusting = actions.isAdjusting,
            adjustmentPreview = adjustment,
            isApplying = actions.isApplying,
            isRefreshing = actions.isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(login = login),
    )

    init {
        viewModelScope.launch {
            refreshContributions(notify = false)
            actionState.update { it.copy(initialRefreshFinished = true) }
            syncSchedule(settingsRepository.current())
        }
    }

    fun updateSettings(transform: (WallpaperSettings) -> WallpaperSettings) {
        viewModelScope.launch {
            syncSchedule(settingsRepository.update(transform))
        }
    }

    fun startAdjusting() {
        actionState.update { it.copy(isAdjusting = true) }
    }

    fun cancelAdjusting() {
        if (actionState.value.isApplying) return
        actionState.update { it.copy(isAdjusting = false) }
    }

    fun applyWallpaper(framing: WallpaperFraming) {
        if (actionState.value.isApplying) return
        actionState.update { it.copy(isApplying = true) }
        viewModelScope.launch {
            val result = try {
                settingsRepository.update { it.copy(framing = framing.coerced()) }
                wallpaperUpdater.update(force = true)
            } finally {
                actionState.update { it.copy(isApplying = false, isAdjusting = false) }
            }
            when (result) {
                UpdateResult.Applied, UpdateResult.Unchanged -> {
                    syncSchedule(settingsRepository.current())
                    _messages.send(HomeMessage.APPLIED)
                }
                is UpdateResult.Failure -> result.error.toMessage()?.let { _messages.send(it) }
            }
        }
    }

    fun refresh() {
        if (actionState.value.isRefreshing) return
        viewModelScope.launch { refreshContributions(notify = true) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private suspend fun refreshContributions(notify: Boolean) {
        actionState.update { it.copy(isRefreshing = true) }
        val result = try {
            contributionRepository.refresh()
        } finally {
            actionState.update { it.copy(isRefreshing = false) }
        }
        if (!notify) return
        val message = when (result) {
            is RefreshResult.Success -> HomeMessage.REFRESHED
            is RefreshResult.Failure -> result.error.toMessage()
        }
        message?.let { _messages.send(it) }
    }

    private suspend fun syncSchedule(settings: WallpaperSettings) {
        when {
            !settings.autoUpdate -> wallpaperScheduler.cancel()
            updateStatusRepository.current().lastAppliedAtMillis != null ->
                wallpaperScheduler.schedule(settings.updateInterval)
        }
    }

    private fun renderPreview(
        calendar: ContributionCalendar,
        settings: WallpaperSettings,
        avatar: Bitmap?,
    ): RenderedPreview {
        val today = LocalDate.now(clock)
        val bitmap = renderer.render(calendar, settings, previewSize, today, avatar)
        val periodStart = DayGrid.from(calendar.days, settings.period, today).startDate
        return RenderedPreview(
            image = bitmap.asImageBitmap(),
            stats = ContributionStats.from(calendar.days, periodStart),
        )
    }

    private fun SyncError.toMessage(): HomeMessage? = when (this) {
        SyncError.NETWORK -> HomeMessage.NETWORK_ERROR
        SyncError.GITHUB -> HomeMessage.GITHUB_ERROR
        SyncError.WALLPAPER -> HomeMessage.WALLPAPER_ERROR
        SyncError.SESSION_EXPIRED -> HomeMessage.SESSION_EXPIRED
        SyncError.NOT_SIGNED_IN -> null
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val PREVIEW_SCALE = 0.4f

        fun factory(container: AppContainer, login: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    login = login,
                    contributionRepository = container.contributionRepository,
                    settingsRepository = container.settingsRepository,
                    updateStatusRepository = container.updateStatusRepository,
                    wallpaperUpdater = container.wallpaperUpdater,
                    wallpaperScheduler = container.wallpaperScheduler,
                    authRepository = container.authRepository,
                    avatarSource = container.avatarSource,
                    renderer = container.wallpaperRenderer,
                    previewSize = container.screenSizeProvider.screenSize().scaled(PREVIEW_SCALE),
                    adjustmentSize = container.screenSizeProvider.screenSize(),
                    clock = container.clock,
                )
            }
        }
    }
}
