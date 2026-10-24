package com.lucasmarujo.gitwallpaper.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucasmarujo.gitwallpaper.R
import com.lucasmarujo.gitwallpaper.domain.CellShape
import com.lucasmarujo.gitwallpaper.domain.ContributionPeriod
import com.lucasmarujo.gitwallpaper.domain.ContributionStats
import com.lucasmarujo.gitwallpaper.domain.UpdateInterval
import com.lucasmarujo.gitwallpaper.domain.WallpaperSettings
import com.lucasmarujo.gitwallpaper.domain.WallpaperTarget
import com.lucasmarujo.gitwallpaper.domain.WallpaperTheme
import com.lucasmarujo.gitwallpaper.ui.components.ContributionPattern
import com.lucasmarujo.gitwallpaper.ui.components.contributionPatternOf
import com.lucasmarujo.gitwallpaper.util.formatMessage
import com.lucasmarujo.gitwallpaper.util.primaryLocale
import com.lucasmarujo.gitwallpaper.wallpaper.WallpaperPalette
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

object HomeTestTags {
    const val APPLY_BUTTON = "home_apply_button"
    const val PREVIEW = "home_preview"
    const val LAST_APPLIED = "home_last_applied"
}

private val ThemeSwatchPattern = contributionPatternOf("142", "031", "204")
private const val COUNT_ARGUMENT = "count"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    showBatteryHint: Boolean,
    onSettingsChange: ((WallpaperSettings) -> WallpaperSettings) -> Unit,
    onApply: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.wallpaper_login, uiState.login),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.home_refresh),
                        )
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = stringResource(R.string.home_sign_out),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                ApplyButton(
                    isApplying = uiState.isApplying,
                    enabled = uiState.hasCalendar,
                    onApply = onApply,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            WallpaperPreview(uiState = uiState, onRetry = onRefresh)
            uiState.stats?.let { StatsRow(it) }
            if (showBatteryHint) {
                BatteryHintCard(onOpenBatterySettings)
            }
            SettingsSection(R.string.home_section_target) {
                ChoiceRow(
                    options = WallpaperTarget.entries,
                    selected = uiState.settings.target,
                    labelRes = { it.labelRes() },
                    onSelect = { target -> onSettingsChange { it.copy(target = target) } },
                )
            }
            SettingsSection(R.string.home_section_theme) {
                ThemeSelector(
                    selected = uiState.settings.theme,
                    onSelect = { theme -> onSettingsChange { it.copy(theme = theme) } },
                )
            }
            SettingsSection(R.string.home_section_period) {
                ChoiceRow(
                    options = ContributionPeriod.entries,
                    selected = uiState.settings.period,
                    labelRes = { it.labelRes() },
                    onSelect = { period -> onSettingsChange { it.copy(period = period) } },
                )
            }
            SettingsSection(R.string.home_section_shape) {
                ChoiceRow(
                    options = CellShape.entries,
                    selected = uiState.settings.cellShape,
                    labelRes = { it.labelRes() },
                    onSelect = { shape -> onSettingsChange { it.copy(cellShape = shape) } },
                )
            }
            SettingsSection(R.string.home_section_position) {
                PositionSlider(
                    value = uiState.settings.verticalPosition,
                    onValueChangeFinished = { position -> onSettingsChange { it.copy(verticalPosition = position) } },
                )
            }
            SwitchRow(
                textRes = R.string.home_show_stats,
                checked = uiState.settings.showStats,
                onCheckedChange = { checked -> onSettingsChange { it.copy(showStats = checked) } },
            )
            SettingsSection(R.string.home_section_auto_update) {
                SwitchRow(
                    textRes = R.string.home_auto_update,
                    checked = uiState.settings.autoUpdate,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(autoUpdate = checked) } },
                )
                IntervalSelector(
                    selected = uiState.settings.updateInterval,
                    enabled = uiState.settings.autoUpdate,
                    onSelect = { interval -> onSettingsChange { it.copy(updateInterval = interval) } },
                )
            }
            LastAppliedText(uiState.lastAppliedAtMillis)
        }
    }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.home_sign_out_title)) },
            text = { Text(stringResource(R.string.home_sign_out_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                ) {
                    Text(stringResource(R.string.home_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.login_cancel))
                }
            },
        )
    }
}

@Composable
private fun ApplyButton(isApplying: Boolean, enabled: Boolean, onApply: () -> Unit) {
    Button(
        onClick = onApply,
        enabled = enabled && !isApplying,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(52.dp)
            .testTag(HomeTestTags.APPLY_BUTTON),
    ) {
        if (isApplying) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.home_applying))
        } else {
            Text(stringResource(R.string.home_apply))
        }
    }
}

@Composable
private fun WallpaperPreview(uiState: HomeUiState, onRetry: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(PREVIEW_WIDTH_FRACTION)
                .aspectRatio(uiState.previewAspectRatio)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .testTag(HomeTestTags.PREVIEW),
            contentAlignment = Alignment.Center,
        ) {
            val preview = uiState.preview
            when {
                preview != null -> Image(
                    bitmap = preview,
                    contentDescription = stringResource(R.string.home_preview_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                uiState.isLoadingCalendar -> CircularProgressIndicator()
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_no_contributions),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.home_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ContributionStats) {
    val resources = LocalResources.current
    val numberFormat = remember(resources.primaryLocale) { NumberFormat.getIntegerInstance(resources.primaryLocale) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(numberFormat.format(stats.totalContributions), R.string.home_stat_contributions)
        StatItem(
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.currentStreak),
            R.string.home_stat_current_streak,
        )
        StatItem(
            resources.formatMessage(R.string.wallpaper_days, COUNT_ARGUMENT to stats.longestStreak),
            R.string.home_stat_longest_streak,
        )
    }
}

@Composable
private fun StatItem(value: String, @StringRes labelRes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryHintCard(onOpenBatterySettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_battery_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.home_battery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onOpenBatterySettings) {
                Text(stringResource(R.string.home_battery_action))
            }
        }
    }
}

@Composable
private fun SettingsSection(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    labelRes: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = stringResource(labelRes(option)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun ThemeSelector(selected: WallpaperTheme, onSelect: (WallpaperTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WallpaperTheme.entries.forEach { theme ->
            val palette = WallpaperPalette.of(theme)
            val isSelected = theme == selected
            val swatchShape = RoundedCornerShape(14.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(theme) })
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(swatchShape)
                        .background(Color(palette.background))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = swatchShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ContributionPattern(
                        palette = palette,
                        pattern = ThemeSwatchPattern,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = stringResource(theme.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun PositionSlider(value: Float, onValueChangeFinished: (Float) -> Unit) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onValueChangeFinished(sliderValue) },
        valueRange = WallpaperSettings.MIN_VERTICAL_POSITION..WallpaperSettings.MAX_VERTICAL_POSITION,
    )
}

@Composable
private fun SwitchRow(@StringRes textRes: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = checked, role = Role.Switch, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalSelector(
    selected: UpdateInterval,
    enabled: Boolean,
    onSelect: (UpdateInterval) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.home_auto_update_every),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UpdateInterval.entries.forEach { interval ->
                FilterChip(
                    selected = interval == selected,
                    onClick = { onSelect(interval) },
                    enabled = enabled,
                    label = { Text(stringResource(interval.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun LastAppliedText(lastAppliedAtMillis: Long?) {
    val locale = LocalResources.current.primaryLocale
    val text = if (lastAppliedAtMillis == null) {
        stringResource(R.string.home_never_applied)
    } else {
        val formatted = remember(lastAppliedAtMillis, locale) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(lastAppliedAtMillis))
        }
        stringResource(R.string.home_last_applied, formatted)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.LAST_APPLIED),
        textAlign = TextAlign.Center,
    )
}

private const val PREVIEW_WIDTH_FRACTION = 0.55f
