package com.arhomedesign.homedesign.photo

import android.graphics.Bitmap
import java.lang.ref.WeakReference
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoRecolorUiState(
    val source: Bitmap? = null,
    val preview: Bitmap? = null,
    val segmentation: SegmentationResult? = null,
    val loading: Boolean = false,
    val selectedRegion: RegionLabel = RegionLabel.WALL,
    val opacity: Float = 0.6f,
    val regionColors: Map<RegionLabel, Color> = RegionLabel.ordered.associateWith { it.defaultColor },
    val isolateClass: RegionLabel? = null,
    val modelLoaded: Boolean = true,
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class PhotoRecolorViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private val modelRunner = SegmentationModelRunner(appContext)
    private val engine: PhotoSegmentationEngine = CompositePhotoSegmentationEngine(modelRunner)
    private val _uiState = MutableStateFlow(PhotoRecolorUiState())
    val uiState: StateFlow<PhotoRecolorUiState> = _uiState.asStateFlow()
    private var sourceRef: WeakReference<Bitmap>? = null
    private val trigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            trigger.debounce(300).collect {
                val state = _uiState.value
                val source = state.source
                val segmentation = state.segmentation
                if (source == null || segmentation == null) return@collect
                val result = kotlinx.coroutines.withContext(Dispatchers.Default) {
                    PhotoRecolorProcessor.composite(
                        source = source,
                        segmentation = segmentation,
                        colors = state.regionColors,
                        opacity = state.opacity,
                        isolateClass = state.isolateClass,
                    )
                }
                _uiState.update { it.copy(preview = result, loading = false) }
            }
        }

    }

    fun loadImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            sourceRef = WeakReference(bitmap)
            _uiState.update { it.copy(loading = true, source = bitmap, preview = bitmap, errorMessage = null) }
            runCatching { engine.segment(bitmap) }
                .onSuccess { segmentation ->
                    _uiState.update { it.copy(segmentation = segmentation, loading = false) }
                    debouncedRecolor()
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(loading = false, errorMessage = throwable.message ?: "Segmentation failed") }
                }
        }
    }

    fun selectRegion(region: RegionLabel) {
        _uiState.update { it.copy(selectedRegion = region) }
    }

    fun setColorForSelected(color: Color) {
        _uiState.update {
            it.copy(regionColors = it.regionColors + (it.selectedRegion to color))
        }
        debouncedRecolor()
    }

    fun setOpacity(value: Float) {
        _uiState.update { it.copy(opacity = value.coerceIn(0.3f, 0.9f)) }
        debouncedRecolor()
    }

    fun setIsolateClass(regionLabel: RegionLabel?) {
        _uiState.update { it.copy(isolateClass = regionLabel) }
        debouncedRecolor()
    }

    private fun debouncedRecolor() {
        _uiState.update { it.copy(loading = true) }
        trigger.tryEmit(Unit)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
