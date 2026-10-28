package com.arhomedesign.ar.ui

import android.app.Activity
import android.content.Context
import com.arhomedesign.ar.data.ARCorePlaneDetector
import com.arhomedesign.ar.data.ArCoreDeviceStatus
import com.arhomedesign.ar.data.ArCoreSupportHelper
import com.google.ar.core.ArCoreApk
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arhomedesign.ar.data.RoomScanRepository
import com.arhomedesign.core.model.OverlayRegion
import com.arhomedesign.core.model.RoomScan
import com.arhomedesign.core.model.DesignVersion
import com.arhomedesign.core.model.MaterialCatalogItem
import com.arhomedesign.design.data.DesignVersionRepository
import com.arhomedesign.design.data.EstimateRepository
import com.arhomedesign.design.data.MaterialCatalogRepository
import com.arhomedesign.design.data.OverlayRegionRepository
import com.arhomedesign.design.data.ShareRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ARScanUiState(
    val arCoreStatus: ArCoreDeviceStatus = ArCoreDeviceStatus.checking(),
    val isLoading: Boolean = false,
    val scans: List<RoomScan> = emptyList(),
    val lastSavedScanId: String? = null,
    val arSessionActive: Boolean = false,
    val detectedPlanes: Int = 0,
    val detectedAnchors: Int = 0,
    val estimatedWidthMeters: Double = 0.0,
    val estimatedLengthMeters: Double = 0.0,
    val estimatedHeightMeters: Double = 2.8,
    val estimatedConfidence: Double = 0.0,
    val selectedSurfaceType: String = "wall",
    val selectedMaterialPreset: String = "default_paint",
    val selectedColorHex: String = "#FFFFFF",
    val activeRoomId: String? = null,
    val previewAppliedOverlayIds: Set<String> = emptySet(),
    val overlays: List<OverlayRegion> = emptyList(),
    val designVersions: List<DesignVersion> = emptyList(),
    val materials: List<MaterialCatalogItem> = emptyList(),
    val estimateSummary: String = "",
    val shareUrl: String = "",
    val compareSummary: String = "",
    val exportSummary: String = "",
    val sharePackageSummary: String = "",
    val canUndoOverlayEdit: Boolean = false,
    val canRedoOverlayEdit: Boolean = false,
    val error: String? = null
)

private data class Quad(
    val scans: List<RoomScan>,
    val overlays: List<OverlayRegion>,
    val versions: List<DesignVersion>,
    val materials: List<MaterialCatalogItem>
)

class ARScanViewModel(
    private val repository: RoomScanRepository,
    private val overlayRepository: OverlayRegionRepository,
    private val designVersionRepository: DesignVersionRepository,
    private val materialCatalogRepository: MaterialCatalogRepository,
    private val estimateRepository: EstimateRepository,
    private val shareRepository: ShareRepository,
    private val planeDetector: ARCorePlaneDetector = ARCorePlaneDetector()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ARScanUiState())
    val uiState: StateFlow<ARScanUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private val overlayUndoStack = ArrayDeque<List<OverlayRegion>>()
    private val overlayRedoStack = ArrayDeque<List<OverlayRegion>>()

    fun loadScans(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val scans = repository.loadRoomScans(projectId)
                val roomId = _uiState.value.activeRoomId ?: scans.firstOrNull()?.id
                val overlays = overlayRepository.loadOverlays(projectId, roomId)
                val versions = designVersionRepository.loadVersions(projectId, roomId)
                val materials = materialCatalogRepository.loadMaterials()
                Quad(scans, overlays, versions, materials)
            }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scans = result.scans,
                        activeRoomId = _uiState.value.activeRoomId ?: result.scans.firstOrNull()?.id,
                        overlays = result.overlays,
                        designVersions = result.versions,
                        materials = result.materials
                    )
                    updateHistoryFlags()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load scans."
                    )
                }
        }
    }

    fun saveScan(
        projectId: String,
        roomName: String,
        widthMeters: Double,
        lengthMeters: Double,
        heightMeters: Double
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val metrics = RoomScanRepository.ScanCaptureMetrics(
                planeCount = _uiState.value.detectedPlanes,
                anchorCount = _uiState.value.detectedAnchors,
                widthMeters = widthMeters,
                lengthMeters = lengthMeters,
                heightMeters = heightMeters,
                confidence = _uiState.value.estimatedConfidence
            )
            runCatching { repository.saveRoomScan(projectId, roomName, metrics) }
                .onSuccess { scanId ->
                    _uiState.value = _uiState.value.copy(
                        lastSavedScanId = scanId,
                        activeRoomId = scanId,
                        isLoading = false
                    )
                    loadScans(projectId)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to save scan."
                    )
                }
        }
    }

    fun refreshArCoreStatus(context: Context) {
        viewModelScope.launch {
            var availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability.name == "UNKNOWN_CHECKING") {
                delay(450)
                availability = ArCoreApk.getInstance().checkAvailability(context)
            }
            val status = ArCoreSupportHelper.fromAvailability(availability)
            _uiState.value = _uiState.value.copy(arCoreStatus = status, error = null)
        }
    }

    fun requestArCoreInstall(activity: Activity) {
        runCatching { ArCoreSupportHelper.requestInstall(activity) }
    }

    fun startArSession(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            var availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability.name == "UNKNOWN_CHECKING") {
                delay(450)
                availability = ArCoreApk.getInstance().checkAvailability(context)
            }
            val status = ArCoreSupportHelper.fromAvailability(availability)
            _uiState.value = _uiState.value.copy(arCoreStatus = status)
            if (!status.canOpenSession) {
                _uiState.value = _uiState.value.copy(arSessionActive = false)
                return@launch
            }
            planeDetector.start(context)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(arSessionActive = true, error = null)
                    startPlanePolling()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        arSessionActive = false,
                        error = throwable.message ?: "Failed to start ARCore session."
                    )
                }
        }
    }

    fun updateDetectedPlanes() {
        val estimate = planeDetector.currentRoomEstimate()
        _uiState.value = _uiState.value.copy(
            detectedPlanes = estimate.planeCount,
            detectedAnchors = estimate.anchorCount,
            estimatedWidthMeters = estimate.widthMeters,
            estimatedLengthMeters = estimate.lengthMeters,
            estimatedHeightMeters = estimate.heightMeters,
            estimatedConfidence = estimate.confidence
        )
    }

    fun selectSurfaceType(surfaceType: String) {
        _uiState.value = _uiState.value.copy(selectedSurfaceType = surfaceType)
    }

    fun selectMaterialPreset(materialPreset: String) {
        _uiState.value = _uiState.value.copy(selectedMaterialPreset = materialPreset)
    }

    fun setColorHex(colorHex: String) {
        _uiState.value = _uiState.value.copy(selectedColorHex = colorHex)
    }

    fun selectActiveRoom(roomId: String) {
        _uiState.value = _uiState.value.copy(activeRoomId = roomId)
    }

    fun addOverlayPrimitive(projectId: String) {
        viewModelScope.launch {
            val roomId = _uiState.value.activeRoomId
            if (roomId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(error = "Create or select a room scan first.")
                return@launch
            }
            val next = _uiState.value.overlays.size + 1
            val overlayName = "Overlay Region $next"
            runCatching {
                overlayRepository.addOverlay(
                    projectId = projectId,
                    roomId = roomId,
                    name = overlayName,
                    surfaceType = _uiState.value.selectedSurfaceType,
                    materialPreset = _uiState.value.selectedMaterialPreset,
                    colorHex = _uiState.value.selectedColorHex
                )
            }.onSuccess {
                loadScans(projectId)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to add overlay.")
            }
        }
    }

    fun toggleMaterialPreview(overlayId: String) {
        val current = _uiState.value.previewAppliedOverlayIds.toMutableSet()
        if (!current.add(overlayId)) current.remove(overlayId)
        _uiState.value = _uiState.value.copy(previewAppliedOverlayIds = current)
    }

    fun clearMaterialPreviewSelection() {
        _uiState.value = _uiState.value.copy(previewAppliedOverlayIds = emptySet())
    }

    fun applyMaterialToPreviewedOverlays(projectId: String) {
        viewModelScope.launch {
            val targetIds = _uiState.value.previewAppliedOverlayIds
            if (targetIds.isEmpty()) return@launch
            snapshotOverlayStateForUndo()
            runCatching {
                targetIds.forEach { overlayId ->
                    overlayRepository.updateOverlayMaterial(
                        overlayId = overlayId,
                        materialPreset = _uiState.value.selectedMaterialPreset,
                        colorHex = _uiState.value.selectedColorHex
                    )
                }
            }.onSuccess {
                loadScans(projectId)
            }.onFailure { throwable ->
                clearRedoIfNeeded()
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to apply batch material.")
            }
        }
    }

    fun saveDesignVersion(projectId: String, notes: String) {
        viewModelScope.launch {
            val roomId = _uiState.value.activeRoomId
            if (roomId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(error = "Select a room before saving version.")
                return@launch
            }
            runCatching {
                designVersionRepository.saveVersion(
                    projectId = projectId,
                    roomId = roomId,
                    notes = notes,
                    overlays = _uiState.value.overlays
                )
            }.onSuccess {
                loadScans(projectId)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to save design version.")
            }
        }
    }

    fun generateEstimate(projectId: String) {
        viewModelScope.launch {
            runCatching {
                estimateRepository.generateEstimate(
                    projectId = projectId,
                    roomId = _uiState.value.activeRoomId,
                    overlays = _uiState.value.overlays
                )
            }.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(estimateSummary = summary)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to generate estimate.")
            }
        }
    }

    fun compareLatestVersions() {
        val versions = _uiState.value.designVersions
        if (versions.size < 2) {
            _uiState.value = _uiState.value.copy(compareSummary = "Need at least two versions to compare.")
            return
        }
        val latest = versions[0]
        val previous = versions[1]
        val summary = "Compare v${latest.versionNumber} vs v${previous.versionNumber}: notes changed from '${previous.notes}' to '${latest.notes}'."
        _uiState.value = _uiState.value.copy(compareSummary = summary)
    }

    fun buildExportSummary(projectId: String) {
        val latestVersion = _uiState.value.designVersions.firstOrNull()
        val summary = buildString {
            appendLine("Project: $projectId")
            appendLine("Room: ${_uiState.value.activeRoomId ?: "N/A"}")
            if (latestVersion != null) {
                appendLine("Version: v${latestVersion.versionNumber}")
                appendLine("Version Notes: ${latestVersion.notes}")
            }
            appendLine("Overlays: ${_uiState.value.overlays.size}")
            appendLine("Estimate: ${_uiState.value.estimateSummary.ifBlank { "Not generated" }}")
        }.trim()
        _uiState.value = _uiState.value.copy(exportSummary = summary)
    }

    fun createShareLink(projectId: String) {
        viewModelScope.launch {
            val versionId = _uiState.value.designVersions.firstOrNull()?.id
            runCatching {
                shareRepository.createShareLink(projectId, versionId)
            }.onSuccess { url ->
                _uiState.value = _uiState.value.copy(shareUrl = url)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to create share link.")
            }
        }
    }

    fun buildSharePackage(projectId: String) {
        viewModelScope.launch {
            runCatching {
                shareRepository.buildSharePackage(projectId, _uiState.value.activeRoomId)
            }.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(sharePackageSummary = summary)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to build share package.")
            }
        }
    }

    fun persistOverlayMaterial(projectId: String, overlayId: String) {
        viewModelScope.launch {
            snapshotOverlayStateForUndo()
            runCatching {
                overlayRepository.updateOverlayMaterial(
                    overlayId = overlayId,
                    materialPreset = _uiState.value.selectedMaterialPreset,
                    colorHex = _uiState.value.selectedColorHex
                )
            }.onSuccess {
                loadScans(projectId)
            }.onFailure { throwable ->
                clearRedoIfNeeded()
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to update material.")
            }
        }
    }

    fun updateOverlayTransform(
        projectId: String,
        overlayId: String,
        deltaX: Double = 0.0,
        deltaY: Double = 0.0,
        deltaZ: Double = 0.0,
        scaleDelta: Double = 0.0,
        rotationDelta: Double = 0.0
    ) {
        viewModelScope.launch {
            val overlay = _uiState.value.overlays.firstOrNull { it.id == overlayId } ?: return@launch
            val nextWidth = (overlay.widthMeters + scaleDelta).coerceAtLeast(0.1)
            val nextHeight = (overlay.heightMeters + scaleDelta).coerceAtLeast(0.1)
            snapshotOverlayStateForUndo()
            runCatching {
                overlayRepository.updateOverlayTransform(
                    overlayId = overlayId,
                    positionX = overlay.positionX + deltaX,
                    positionY = overlay.positionY + deltaY,
                    positionZ = overlay.positionZ + deltaZ,
                    widthMeters = nextWidth,
                    heightMeters = nextHeight,
                    rotationDeg = overlay.rotationDeg + rotationDelta
                )
            }.onSuccess {
                loadScans(projectId)
            }.onFailure { throwable ->
                clearRedoIfNeeded()
                _uiState.value = _uiState.value.copy(error = throwable.message ?: "Failed to update transform.")
            }
        }
    }

    fun undoOverlayEdits(projectId: String) {
        viewModelScope.launch {
            if (overlayUndoStack.isEmpty()) return@launch
            val current = _uiState.value.overlays
            overlayRedoStack.addLast(current)
            val previous = overlayUndoStack.removeLast()
            applyOverlayState(projectId, previous)
            updateHistoryFlags()
        }
    }

    fun redoOverlayEdits(projectId: String) {
        viewModelScope.launch {
            if (overlayRedoStack.isEmpty()) return@launch
            val current = _uiState.value.overlays
            overlayUndoStack.addLast(current)
            val next = overlayRedoStack.removeLast()
            applyOverlayState(projectId, next)
            updateHistoryFlags()
        }
    }

    private suspend fun applyOverlayState(projectId: String, overlays: List<OverlayRegion>) {
        overlays.forEach { overlay ->
            overlayRepository.updateOverlayMaterial(overlay.id, overlay.materialPreset, overlay.colorHex)
            overlayRepository.updateOverlayTransform(
                overlayId = overlay.id,
                positionX = overlay.positionX,
                positionY = overlay.positionY,
                positionZ = overlay.positionZ,
                widthMeters = overlay.widthMeters,
                heightMeters = overlay.heightMeters,
                rotationDeg = overlay.rotationDeg
            )
        }
        loadScans(projectId)
    }

    private fun snapshotOverlayStateForUndo() {
        overlayUndoStack.addLast(_uiState.value.overlays)
        overlayRedoStack.clear()
        updateHistoryFlags()
    }

    private fun clearRedoIfNeeded() {
        overlayRedoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _uiState.value = _uiState.value.copy(
            canUndoOverlayEdit = overlayUndoStack.isNotEmpty(),
            canRedoOverlayEdit = overlayRedoStack.isNotEmpty()
        )
    }

    fun stopArSession() {
        pollingJob?.cancel()
        pollingJob = null
        planeDetector.stop()
        _uiState.value = _uiState.value.copy(arSessionActive = false, detectedPlanes = 0, detectedAnchors = 0)
    }

    private fun startPlanePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (_uiState.value.arSessionActive) {
                updateDetectedPlanes()
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        planeDetector.stop()
        super.onCleared()
    }
}
