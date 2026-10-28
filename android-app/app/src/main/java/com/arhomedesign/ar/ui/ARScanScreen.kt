package com.arhomedesign.ar.ui

import android.content.Context
import android.graphics.Color.parseColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arhomedesign.core.model.OverlayRegion
import com.arhomedesign.core.model.RoomScan
import com.arhomedesign.core.ui.components.AppOutlinedButton
import com.arhomedesign.core.ui.components.AppPrimaryButton
import com.arhomedesign.core.ui.components.AppSecondaryButton
import com.arhomedesign.core.ui.components.ErrorBanner
import com.arhomedesign.core.ui.components.PremiumScreenBackground
import com.arhomedesign.core.ui.components.SectionCard

private enum class StudioTab(val label: String, val hint: String) {
    Plan("Plan", "Room size, scans & camera AR"),
    Decorate("Decorate", "Surfaces, materials & overlays"),
    Finish("Finish", "Versions, estimates & sharing")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScanScreen(
    context: Context,
    projectId: String,
    state: ARScanUiState,
    onBack: () -> Unit,
    onRefreshArCoreStatus: () -> Unit,
    onInstallArCore: () -> Unit,
    onStartArSession: (Context) -> Unit,
    onUpdateDetectedPlanes: () -> Unit,
    onCaptureFromArCore: (String, Double, Double, Double) -> Unit,
    onSelectActiveRoom: (String) -> Unit,
    onSelectSurfaceType: (String) -> Unit,
    onSelectMaterialPreset: (String) -> Unit,
    onSetColorHex: (String) -> Unit,
    onAddOverlayPrimitive: () -> Unit,
    onSaveDesignVersion: (String) -> Unit,
    onGenerateEstimate: () -> Unit,
    onCompareVersions: () -> Unit,
    onBuildExportSummary: () -> Unit,
    onCreateShareLink: () -> Unit,
    onBuildSharePackage: () -> Unit,
    onToggleMaterialPreview: (String) -> Unit,
    onApplyMaterialToPreviewed: () -> Unit,
    onClearPreviewSelection: () -> Unit,
    onPersistOverlayMaterial: (String) -> Unit,
    onAdjustOverlayTransform: (String, Double, Double, Double, Double, Double) -> Unit,
    onUndoOverlayEdits: () -> Unit,
    onRedoOverlayEdits: () -> Unit,
    onStopArSession: () -> Unit,
    onRefresh: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val roomName = remember { mutableStateOf("Living Room") }
    val widthMeters = remember { mutableStateOf("4.0") }
    val lengthMeters = remember { mutableStateOf("5.0") }
    val heightMeters = remember { mutableStateOf("2.8") }
    val versionNotes = remember { mutableStateOf("Design checkpoint") }

    LaunchedEffect(
        state.estimatedWidthMeters,
        state.estimatedLengthMeters,
        state.estimatedHeightMeters
    ) {
        if (state.estimatedWidthMeters > 0) widthMeters.value = "%.2f".format(state.estimatedWidthMeters)
        if (state.estimatedLengthMeters > 0) lengthMeters.value = "%.2f".format(state.estimatedLengthMeters)
        if (state.estimatedHeightMeters > 0) heightMeters.value = "%.2f".format(state.estimatedHeightMeters)
    }

    val fieldShape = MaterialTheme.shapes.medium
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.secondary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.secondary
    )

    val dimsValid = listOf(widthMeters.value, lengthMeters.value, heightMeters.value)
        .all { it.toDoubleOrNull() != null && (it.toDoubleOrNull() ?: 0.0) > 0 }

    PremiumScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Design studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Project · ${projectId.take(8)}…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StudioTabStrip(
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }

                item {
                    val tab = StudioTab.entries[selectedTab]
                    Text(
                        text = tab.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                when (selectedTab) {
                    0 -> planTabContent(
                        state = state,
                        context = context,
                        roomName = roomName.value,
                        onRoomNameChange = { roomName.value = it },
                        widthMeters = widthMeters.value,
                        onWidthChange = { widthMeters.value = it },
                        lengthMeters = lengthMeters.value,
                        onLengthChange = { lengthMeters.value = it },
                        heightMeters = heightMeters.value,
                        onHeightChange = { heightMeters.value = it },
                        fieldShape = fieldShape,
                        fieldColors = fieldColors,
                        dimsValid = dimsValid,
                        onRefreshArCoreStatus = onRefreshArCoreStatus,
                        onInstallArCore = onInstallArCore,
                        onStartArSession = onStartArSession,
                        onUpdateDetectedPlanes = onUpdateDetectedPlanes,
                        onStopArSession = onStopArSession,
                        onRefresh = onRefresh,
                        onCaptureFromArCore = onCaptureFromArCore,
                        onAddOverlayPrimitive = onAddOverlayPrimitive,
                        onUndoOverlayEdits = onUndoOverlayEdits,
                        onRedoOverlayEdits = onRedoOverlayEdits,
                        onApplyMaterialToPreviewed = onApplyMaterialToPreviewed,
                        onClearPreviewSelection = onClearPreviewSelection,
                        onSelectActiveRoom = onSelectActiveRoom
                    )
                    1 -> decorateTabContent(
                        state = state,
                        fieldShape = fieldShape,
                        fieldColors = fieldColors,
                        onSelectSurfaceType = onSelectSurfaceType,
                        onSelectMaterialPreset = onSelectMaterialPreset,
                        onSetColorHex = onSetColorHex,
                        onToggleMaterialPreview = onToggleMaterialPreview,
                        onPersistOverlayMaterial = onPersistOverlayMaterial,
                        onAdjustOverlayTransform = onAdjustOverlayTransform
                    )
                    else -> finishTabContent(
                        state = state,
                        versionNotes = versionNotes.value,
                        onVersionNotesChange = { versionNotes.value = it },
                        fieldShape = fieldShape,
                        fieldColors = fieldColors,
                        onSaveDesignVersion = onSaveDesignVersion,
                        onGenerateEstimate = onGenerateEstimate,
                        onCompareVersions = onCompareVersions,
                        onBuildExportSummary = onBuildExportSummary,
                        onCreateShareLink = onCreateShareLink,
                        onBuildSharePackage = onBuildSharePackage
                    )
                }

                item {
                    state.error?.let { ErrorBanner(message = it) }
                }
            }
        }
    }
}

@Composable
private fun StudioTabStrip(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioTab.entries.forEachIndexed { index, tab ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

private fun LazyListScope.planTabContent(
    state: ARScanUiState,
    context: Context,
    roomName: String,
    onRoomNameChange: (String) -> Unit,
    widthMeters: String,
    onWidthChange: (String) -> Unit,
    lengthMeters: String,
    onLengthChange: (String) -> Unit,
    heightMeters: String,
    onHeightChange: (String) -> Unit,
    fieldShape: Shape,
    fieldColors: TextFieldColors,
    dimsValid: Boolean,
    onRefreshArCoreStatus: () -> Unit,
    onInstallArCore: () -> Unit,
    onStartArSession: (Context) -> Unit,
    onUpdateDetectedPlanes: () -> Unit,
    onStopArSession: () -> Unit,
    onRefresh: () -> Unit,
    onCaptureFromArCore: (String, Double, Double, Double) -> Unit,
    onAddOverlayPrimitive: () -> Unit,
    onUndoOverlayEdits: () -> Unit,
    onRedoOverlayEdits: () -> Unit,
    onApplyMaterialToPreviewed: () -> Unit,
    onClearPreviewSelection: () -> Unit,
    onSelectActiveRoom: (String) -> Unit
) {
    item {
        SectionCard(title = "Room measurements") {
            SectionHelper("Used when saving a scan and for estimates. You can type sizes by hand anytime.")
            OutlinedTextField(
                value = roomName,
                onValueChange = onRoomNameChange,
                label = { Text("Room name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors
            )
            StudioLabeledField("Width (meters)") {
                OutlinedTextField(
                    value = widthMeters,
                    onValueChange = onWidthChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    placeholder = { Text("e.g. 4.0") }
                )
            }
            StudioLabeledField("Length (meters)") {
                OutlinedTextField(
                    value = lengthMeters,
                    onValueChange = onLengthChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    placeholder = { Text("e.g. 5.0") }
                )
            }
            StudioLabeledField("Height (meters)") {
                OutlinedTextField(
                    value = heightMeters,
                    onValueChange = onHeightChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    placeholder = { Text("e.g. 2.8") }
                )
            }
        }
    }

    item {
        SectionCard(title = "Saved rooms") {
            SectionHelper("Choose which room overlays and materials apply to.")
            if (state.scans.isEmpty()) {
                Text(
                    "No scans yet. Save one below after AR or with manual sizes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    items(state.scans, key = { it.id }) { scan ->
        ScanCard(
            scan = scan,
            isActive = state.activeRoomId == scan.id,
            onSelect = { onSelectActiveRoom(scan.id) }
        )
    }

    item {
        SectionCard(title = "Camera AR") {
            ArCoreStatusBanner(
                status = state.arCoreStatus,
                onRefreshStatus = onRefreshArCoreStatus,
                onInstallArCore = onInstallArCore
            )
            if (!state.arSessionActive) {
                AppPrimaryButton(
                    text = "Start live AR",
                    onClick = { onStartArSession(context) },
                    enabled = state.arCoreStatus.canOpenSession
                )
            } else {
                Text(
                    "Live camera AR is on",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                ARCameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            if (!state.arCoreStatus.canOpenSession && !state.arSessionActive) {
                Text(
                    "You can still use this screen: enter room sizes at the top of Plan and save scans without AR. " +
                        "If you installed Google Play Services for AR from the Play Store, tap Refresh status above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppSecondaryButton(
                text = "Update planes & room estimate",
                onClick = onUpdateDetectedPlanes,
                enabled = state.arSessionActive
            )
            TelemetryPanel(state = state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppOutlinedButton(
                    text = "Stop AR",
                    onClick = onStopArSession,
                    enabled = state.arSessionActive,
                    modifier = Modifier.weight(1f)
                )
                AppOutlinedButton(
                    text = "Reload data",
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    item {
        SectionCard(title = "Save & structure") {
            SectionHelper("Save a scan to this project. Works best with AR on; manual sizes still save if AR is off.")
            AppPrimaryButton(
                text = "Save room scan",
                onClick = {
                    onCaptureFromArCore(
                        roomName,
                        widthMeters.toDoubleOrNull() ?: 0.0,
                        lengthMeters.toDoubleOrNull() ?: 0.0,
                        heightMeters.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = dimsValid && roomName.isNotBlank()
            )
            AppSecondaryButton(text = "Add overlay region", onClick = onAddOverlayPrimitive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "Undo",
                    onClick = onUndoOverlayEdits,
                    enabled = state.canUndoOverlayEdit,
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Redo",
                    onClick = onRedoOverlayEdits,
                    enabled = state.canRedoOverlayEdit,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppOutlinedButton(
                    text = "Apply material (preview)",
                    onClick = onApplyMaterialToPreviewed,
                    modifier = Modifier.weight(1f)
                )
                AppOutlinedButton(
                    text = "Clear preview",
                    onClick = onClearPreviewSelection,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun LazyListScope.decorateTabContent(
    state: ARScanUiState,
    fieldShape: Shape,
    fieldColors: TextFieldColors,
    onSelectSurfaceType: (String) -> Unit,
    onSelectMaterialPreset: (String) -> Unit,
    onSetColorHex: (String) -> Unit,
    onToggleMaterialPreview: (String) -> Unit,
    onPersistOverlayMaterial: (String) -> Unit,
    onAdjustOverlayTransform: (String, Double, Double, Double, Double, Double) -> Unit
) {
    item {
        SectionCard(title = "Surface") {
            SectionHelper("What you’re painting or covering.")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("wall" to "Wall", "floor" to "Floor", "ceiling" to "Ceiling").forEach { (key, label) ->
                    FilterChip(
                        selected = state.selectedSurfaceType == key,
                        onClick = { onSelectSurfaceType(key) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }

    item {
        SectionCard(title = "Materials") {
            SectionHelper("Quick picks and catalog. Active room: ${state.activeRoomId?.take(8)?.plus("…") ?: "pick a room in Plan"}")
            Text(
                "Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppSecondaryButton(
                    text = "Paint",
                    onClick = { onSelectMaterialPreset("default_paint") },
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Oak",
                    onClick = { onSelectMaterialPreset("oak_wood") },
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Marble",
                    onClick = { onSelectMaterialPreset("marble_tile") },
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.materials.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    "Catalog",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.materials.forEach { material ->
                    AppOutlinedButton(
                        text = "${material.name} · ${material.category}",
                        onClick = {
                            onSelectMaterialPreset(material.presetCode)
                            onSetColorHex(material.colorHex)
                        }
                    )
                }
            }
            StudioLabeledField("Accent color (hex)") {
                OutlinedTextField(
                    value = state.selectedColorHex,
                    onValueChange = onSetColorHex,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    placeholder = { Text("#FFFFFF") }
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    "Selected preset: ${state.selectedMaterialPreset}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    item {
        SectionCard(title = "Overlay regions") {
            SectionHelper("Each overlay is an area you can style. Use Preview to try a material, then Save to store it.")
            if (state.overlays.isEmpty()) {
                Text(
                    "No overlays yet. Add one from the Plan tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    items(state.overlays, key = { it.id }) { overlay ->
        OverlayCard(
            overlay = overlay,
            state = state,
            onToggleMaterialPreview = onToggleMaterialPreview,
            onPersistOverlayMaterial = onPersistOverlayMaterial,
            onAdjustOverlayTransform = onAdjustOverlayTransform
        )
    }
}

private fun LazyListScope.finishTabContent(
    state: ARScanUiState,
    versionNotes: String,
    onVersionNotesChange: (String) -> Unit,
    fieldShape: Shape,
    fieldColors: TextFieldColors,
    onSaveDesignVersion: (String) -> Unit,
    onGenerateEstimate: () -> Unit,
    onCompareVersions: () -> Unit,
    onBuildExportSummary: () -> Unit,
    onCreateShareLink: () -> Unit,
    onBuildSharePackage: () -> Unit
) {
    item {
        SectionCard(title = "Versions") {
            SectionHelper("Save snapshots you can compare later.")
            OutlinedTextField(
                value = versionNotes,
                onValueChange = onVersionNotesChange,
                label = { Text("Notes for this version") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = fieldShape,
                colors = fieldColors
            )
            AppPrimaryButton(
                text = "Save version",
                onClick = { onSaveDesignVersion(versionNotes) }
            )
        }
    }

    item {
        SectionCard(title = "Estimates & compare") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "Get estimate",
                    onClick = onGenerateEstimate,
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Compare versions",
                    onClick = onCompareVersions,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.estimateSummary.isNotBlank()) {
                SummaryBlock(label = "Estimate", body = state.estimateSummary)
            }
            if (state.compareSummary.isNotBlank()) {
                SummaryBlock(label = "Comparison", body = state.compareSummary)
            }
            AppOutlinedButton(text = "Build export summary", onClick = onBuildExportSummary)
            if (state.exportSummary.isNotBlank()) {
                SummaryBlock(label = "Export", body = state.exportSummary)
            }
        }
    }

    item {
        SectionCard(title = "Share") {
            SectionHelper("Create a link or a structured package for collaborators.")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "Share link",
                    onClick = onCreateShareLink,
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Share package",
                    onClick = onBuildSharePackage,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.shareUrl.isNotBlank()) {
                SummaryBlock(label = "Link", body = state.shareUrl)
            }
            if (state.sharePackageSummary.isNotBlank()) {
                SummaryBlock(label = "Package", body = state.sharePackageSummary)
            }
        }
    }

    item {
        SectionCard(title = "Version history") {
            if (state.designVersions.isEmpty()) {
                Text(
                    "Saved versions will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    items(
        state.designVersions,
        key = { v -> v.id.ifBlank { "${v.projectId}-${v.versionNumber}" } }
    ) { version ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "v${version.versionNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    version.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHelper(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun StudioLabeledField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun ScanCard(
    scan: RoomScan,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(scan.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isActive) {
                    Text(
                        "Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                "${scan.widthMeters} × ${scan.lengthMeters} × ${scan.heightMeters} m · confidence ${scan.confidence}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${scan.planeCount} planes · ${scan.anchorCount} anchors · ${scan.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppPrimaryButton(
                text = if (isActive) "Selected for overlays" else "Use for overlays",
                onClick = onSelect,
                enabled = !isActive
            )
        }
    }
}

@Composable
private fun TelemetryPanel(state: ARScanUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "Live metrics",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val lines = listOf(
            "Tracked planes" to "${state.detectedPlanes}",
            "Anchors" to "${state.detectedAnchors}",
            "Estimated size (m)" to "%.2f × %.2f × %.2f".format(
                state.estimatedWidthMeters,
                state.estimatedLengthMeters,
                state.estimatedHeightMeters
            ),
            "Confidence" to "%.2f".format(state.estimatedConfidence)
        )
        lines.forEach { (k, v) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    k,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    v,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SummaryBlock(label: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OverlayCard(
    overlay: OverlayRegion,
    state: ARScanUiState,
    onToggleMaterialPreview: (String) -> Unit,
    onPersistOverlayMaterial: (String) -> Unit,
    onAdjustOverlayTransform: (String, Double, Double, Double, Double, Double) -> Unit
) {
    val previewTint = runCatching { Color(parseColor(overlay.colorHex)) }.getOrElse { MaterialTheme.colorScheme.secondary }
    val highlighted = state.previewAppliedOverlayIds.contains(overlay.id)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                previewTint.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(overlay.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${overlay.surfaceType.replaceFirstChar { it.titlecase() }} · ${overlay.materialPreset} · ${overlay.colorHex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Position & size: ${"%.2f".format(overlay.widthMeters)}×${"%.2f".format(overlay.heightMeters)} m · ${"%.0f".format(overlay.rotationDeg)}°",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (highlighted) "Preview active" else "Preview off",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppSecondaryButton(
                    text = "Preview",
                    onClick = { onToggleMaterialPreview(overlay.id) },
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Save look",
                    onClick = { onPersistOverlayMaterial(overlay.id) },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Adjust",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOutlinedButton(
                    text = "Move +",
                    onClick = { onAdjustOverlayTransform(overlay.id, 0.1, 0.0, 0.0, 0.0, 0.0) },
                    modifier = Modifier.weight(1f)
                )
                AppOutlinedButton(
                    text = "Scale +",
                    onClick = { onAdjustOverlayTransform(overlay.id, 0.0, 0.0, 0.0, 0.1, 0.0) },
                    modifier = Modifier.weight(1f)
                )
                AppOutlinedButton(
                    text = "Rotate",
                    onClick = { onAdjustOverlayTransform(overlay.id, 0.0, 0.0, 0.0, 0.0, 15.0) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
