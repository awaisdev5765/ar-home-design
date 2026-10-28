package com.arhomedesign.homedesign

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.arhomedesign.core.data.AppPreferences
import com.arhomedesign.homedesign.data.HomeDesignRepository
import com.arhomedesign.homedesign.data.SavedPalette
import com.arhomedesign.homedesign.brand.FinishType
import com.arhomedesign.homedesign.brand.PaintBrandColor
import com.arhomedesign.homedesign.brand.asColor
import com.arhomedesign.homedesign.brand.samplePaintBrands
import com.arhomedesign.homedesign.retention.RetentionScheduler
import com.arhomedesign.homedesign.state.ArTrackingStatus
import com.arhomedesign.homedesign.state.HomeDesignState
import com.arhomedesign.homedesign.state.UiState
import com.arhomedesign.paintpreview.PaintSurfaceFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeDesignViewModel @Inject constructor(
    private val repository: HomeDesignRepository,
    private val preferences: AppPreferences,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeDesignState>>(UiState.Success(HomeDesignState()))
    val uiState: StateFlow<UiState<HomeDesignState>> = _uiState.asStateFlow()
    private val _savedPalettes = MutableStateFlow<List<SavedPalette>>(emptyList())
    val savedPalettes: StateFlow<List<SavedPalette>> = _savedPalettes.asStateFlow()
    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    private var recentlyDeletedPalette: SavedPalette? = null

    init {
        viewModelScope.launch {
            repository.observeSavedPalettes().collect { _savedPalettes.value = it }
        }
        viewModelScope.launch {
            preferences.markAppOpened(LocalDate.now())
            preferences.openDates.collect { _streakDays.value = computeStreak(it) }
        }
        viewModelScope.launch {
            preferences.notificationOptIn.collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            preferences.lastOpenAt.collect { last ->
                if (last <= 0L) return@collect
                val sevenDays = 7L * 24L * 60L * 60L * 1000L
                if (System.currentTimeMillis() - last > sevenDays) {
                    RetentionScheduler.cancel(appContext)
                }
            }
        }
    }

    fun setColor(color: Color) = updateSuccess { copy(selectedColor = color, selectedColorHex = color.toHex6()) }
    fun setHex(hex: String) = updateSuccess { copy(selectedColorHex = hex) }
    fun setHexAndColor(hex: String) {
        setHex(hex)
        runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()?.let(::setColor)
    }
    fun setOpacity(value: Float) = updateSuccess { copy(tintOpacity = value) }
    fun setFilter(filter: PaintSurfaceFilter) = updateSuccess { copy(selectedFilter = filter) }
    fun setWallSensitivity(value: Float) = updateSuccess { copy(wallSensitivity = value) }
    fun setOutlines(show: Boolean) = updateSuccess { copy(showOutlines = show) }
    fun toggleAdvanced() = updateSuccess { copy(advancedExpanded = !advancedExpanded) }
    fun setTracking(status: ArTrackingStatus) = updateSuccess { copy(trackingStatus = status) }
    fun setPlaneCount(count: Int) = updateSuccess { copy(planeCount = count) }
    fun dismissCoachingAfterDetection() = updateSuccess {
        if (coachingVisible) copy(coachingVisible = false) else this
    }
    fun setFinishType(finishType: FinishType) = updateSuccess {
        copy(finishType = finishType, tintOpacity = finishType.targetOpacity)
    }
    fun selectBrandColor(brandName: String, brandColor: PaintBrandColor) = updateSuccess {
        copy(
            selectedColor = brandColor.asColor(),
            selectedColorHex = brandColor.hexColor,
            selectedBrandName = brandName,
            selectedProductName = brandColor.name,
            selectedProductCode = brandColor.productCode,
            finishType = brandColor.finishType,
            tintOpacity = brandColor.finishType.targetOpacity,
        )
    }
    fun applyPalette(palette: SavedPalette) {
        val firstColor = palette.colors.firstOrNull() ?: return
        setColor(firstColor)
    }
    fun toggleFavorite(palette: SavedPalette) {
        viewModelScope.launch { repository.setFavorite(palette.id, !palette.isFavorite) }
    }

    fun autoSaveCurrentPalette() {
        val state = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.savePalette(
                name = "Palette ${System.currentTimeMillis().toString().takeLast(4)}",
                colors = listOf(state.selectedColor),
            )
        }
    }

    fun deletePaletteWithUndo(palette: SavedPalette) {
        recentlyDeletedPalette = palette
        viewModelScope.launch { repository.deletePalette(palette.id) }
    }

    fun undoDeletePalette() {
        val deleted = recentlyDeletedPalette ?: return
        recentlyDeletedPalette = null
        viewModelScope.launch {
            repository.savePalette(deleted.name, deleted.colors, isFavorite = deleted.isFavorite)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferences.setOnboardingComplete(true) }
    }

    fun setDailyNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationOptIn(enabled)
            _notificationsEnabled.value = enabled
            if (enabled) {
                RetentionScheduler.scheduleDaily(appContext)
            } else {
                RetentionScheduler.cancel(appContext)
            }
        }
    }

    fun colorOfTheDay(): Triple<String, PaintBrandColor, String> {
        val flat = samplePaintBrands.flatMap { b -> b.colors.map { b.name to it } }
        val index = LocalDate.now().dayOfYear % flat.size
        val (brand, color) = flat[index]
        return Triple(brand, color, color.hexColor)
    }

    fun resetAllColors() = updateSuccess {
        HomeDesignState(
            trackingStatus = trackingStatus,
            planeCount = planeCount,
            activeModeLabel = activeModeLabel,
            coachingVisible = coachingVisible,
        )
    }

    private inline fun updateSuccess(block: HomeDesignState.() -> HomeDesignState) {
        _uiState.update { current ->
            when (current) {
                is UiState.Success -> UiState.Success(current.data.block())
                else -> current
            }
        }
    }
}

private fun Color.toHex6(): String {
    val rgb = (toArgb() and 0xFFFFFF)
    return String.format("%06X", rgb)
}

private fun computeStreak(openDates: Set<String>): Int {
    if (openDates.isEmpty()) return 0
    var streak = 0
    var cursor = LocalDate.now()
    val set = openDates.toSet()
    while (set.contains(cursor.toString())) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
