package com.arhomedesign.paintpreview.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arhomedesign.ar.data.ArCoreSupportHelper
import com.arhomedesign.homedesign.HomeDesignViewModel
import com.arhomedesign.homedesign.brand.FinishType
import com.arhomedesign.homedesign.state.ArTrackingStatus
import com.arhomedesign.homedesign.state.UiState
import com.arhomedesign.paintpreview.PaintSurfaceFilter
import com.arhomedesign.paintpreview.ml.ARSegmentationProcessor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val premiumPalette = listOf(
    Color(0xFFF9F9F6), Color(0xFFE8E0D5), Color(0xFFD7D7D2), Color(0xFF7EB6D9), Color(0xFF3D7A8C),
    Color(0xFF2A7A7B), Color(0xFF8FAE8B), Color(0xFFE8A838), Color(0xFFC4705A), Color(0xFFB8A0D8),
    Color(0xFF4A6FA5), Color(0xFF1F6F54), Color(0xFF9B2335), Color(0xFF5C6670), Color(0xFF1A1A1C),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PaintPreviewScreen(
    onBack: (() -> Unit)? = null,
    onOpenBrandPicker: () -> Unit = {},
    onOpenPhotoRecolor: () -> Unit = {},
    viewModel: HomeDesignViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val savedPalettes by viewModel.savedPalettes.collectAsState()
    val state = (uiState as? UiState.Success)?.data ?: return
    val segmentationProcessor = remember { ARSegmentationProcessor(context) }
    val arStatus = remember { ArCoreSupportHelper.checkStatus(context) }
    val wallSegmentation by segmentationProcessor.segmentation.collectAsState()
    val inferenceMs by segmentationProcessor.inferenceMs.collectAsState()
    var wallAiMode by remember(segmentationProcessor.wallAiAvailable) { mutableStateOf(segmentationProcessor.wallAiAvailable) }
    var fps by remember { mutableStateOf(0) }
    var memMb by remember { mutableStateOf(0L) }

    var session by remember { mutableStateOf<Session?>(null) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val paletteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var glViewRef by remember { mutableStateOf<PaintArGlSurfaceView?>(null) }
    var selectedSwatch by remember { mutableIntStateOf(3) }
    var showPaletteLoader by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var paintFocusActive by remember { mutableStateOf(false) }
    var popupSwatch by remember { mutableStateOf<Color?>(null) }
    val haptics = LocalHapticFeedback.current

    val selectedColor by remember(state.selectedColor, state.tintOpacity) {
        derivedStateOf { state.selectedColor.copy(alpha = state.tintOpacity) }
    }
    val animatedWallTint by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = tween(durationMillis = 300),
        label = "wallTint",
    )
    LaunchedEffect(state.selectedColor) {
        val idx = premiumPalette.indexOfFirst { it.toArgb() == state.selectedColor.toArgb() }
        if (idx >= 0) selectedSwatch = idx
    }
    val colorArgb by remember(selectedColor) { derivedStateOf { selectedColor.toArgb() } }
    val wallMaskOverlay by remember(wallSegmentation, state.selectedColor, state.tintOpacity, wallAiMode) {
        mutableStateOf(
            if (wallAiMode && wallSegmentation != null) {
                buildWallMaskBitmap(
                    segmentation = wallSegmentation!!,
                    tintColor = animatedWallTint,
                    opacity = state.tintOpacity,
                ).asImageBitmap()
            } else {
                null
            }
        )
    }
    val latestSession by rememberUpdatedState(session)
    val latestGlView by rememberUpdatedState(glViewRef)
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
    val dashAnim = rememberInfiniteTransition(label = "dashAnim")
    val dashPhase by dashAnim.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(animation = tween(1200)),
        label = "dashPhase",
    )

    LaunchedEffect(Unit) {
        if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(cameraGranted) {
        if (!cameraGranted || !ArCoreSupportHelper.checkStatus(context).canOpenSession) return@LaunchedEffect
        if (session != null) return@LaunchedEffect
        session = runCatching {
            Session(context).apply {
                val cfg = Config(this).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    textureUpdateMode = Config.TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES
                    focusMode = Config.FocusMode.AUTO
                }
                configure(cfg)
            }
        }.getOrNull()
    }

    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        var frames = 0
        var mark = System.currentTimeMillis()
        while (isActive) {
            delay(60)
            frames++
            val now = System.currentTimeMillis()
            if (now - mark >= 1000L) {
                fps = frames
                frames = 0
                mark = now
            }
            val planes = s.getAllTrackables(Plane::class.java)
            val tracked = planes.count { it.trackingState == TrackingState.TRACKING || it.trackingState == TrackingState.PAUSED }
            viewModel.setPlaneCount(tracked)
            if (tracked > 0) viewModel.dismissCoachingAfterDetection()
            // Renderer already calls Session.update() on the GL thread.
            // Calling update() from this coroutine can race and crash ARCore on some devices.
            val camTracking = if (tracked > 0) TrackingState.TRACKING else TrackingState.PAUSED
            viewModel.setTracking(
                when (camTracking) {
                    TrackingState.TRACKING -> ArTrackingStatus.TRACKING
                    TrackingState.PAUSED -> ArTrackingStatus.LIMITED
                    else -> ArTrackingStatus.NONE
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            val used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            memMb = used / (1024L * 1024L)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val s = latestSession ?: return@LifecycleEventObserver
            val v = latestGlView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    runCatching { s.resume() }
                    v.onHostResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    v.onHostPause()
                    runCatching { s.pause() }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    v.onHostPause()
                    runCatching { s.pause() }
                    runCatching { s.close() }
                }
                Lifecycle.Event.ON_START -> segmentationProcessor.setActive(true)
                Lifecycle.Event.ON_STOP -> segmentationProcessor.setActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.autoSaveCurrentPalette()
            segmentationProcessor.stop()
        }
    }

    DisposableEffect(glViewRef) {
        val view = glViewRef ?: return@DisposableEffect onDispose { }
        val renderer = view.paintRenderer
        renderer.paintFocusActiveListener = { active ->
            paintFocusActive = active
            if (active) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40L)
                }
            }
        }
        onDispose {
            renderer.paintFocusActiveListener = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenPhotoRecolor) {
                Icon(Icons.Rounded.Camera, contentDescription = "Capture")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cameraGranted && session != null) {
                AndroidView(
                    factory = { ctx ->
                        PaintArGlSurfaceView(ctx).also {
                            it.attachSession(session!!)
                            glViewRef = it
                        }
                    },
                    update = { view ->
                        view.queueEvent {
                            view.paintRenderer.paintColorArgb = colorArgb
                            view.paintRenderer.wallPaintArgb = colorArgb
                            view.paintRenderer.floorPaintArgb = colorArgb
                            view.paintRenderer.ceilingPaintArgb = colorArgb
                            view.paintRenderer.surfaceFilter = state.selectedFilter
                            view.paintRenderer.wallNormalThreshold = state.wallSensitivity
                            view.paintRenderer.drawSurfaceOutlines = state.showOutlines
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        val message = if (!cameraGranted) {
                            "Camera permission is required for AR preview."
                        } else if (!arStatus.canOpenSession) {
                            "ARCore is unavailable on this device. You can still use Photo Recolor."
                        } else {
                            "Preparing camera..."
                        }
                        Text(message)
                        if (!cameraGranted) {
                            Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant camera permission")
                            }
                        }
                        Button(onClick = onOpenPhotoRecolor) {
                            Text("Open Photo Recolor")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val statusColor = when (state.trackingStatus) {
                            ArTrackingStatus.TRACKING -> Color(0xFF24C16A)
                            ArTrackingStatus.LIMITED -> Color(0xFFFFB020)
                            ArTrackingStatus.NONE -> Color(0xFFE5484D)
                        }
                        Box(Modifier.size(10.dp).background(statusColor, CircleShape))
                        Text(state.activeModeLabel, style = MaterialTheme.typography.labelLarge)
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "${state.planeCount} surfaces detected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        FilterChip(
                            selected = wallAiMode,
                            onClick = { if (segmentationProcessor.wallAiAvailable) wallAiMode = !wallAiMode },
                            enabled = segmentationProcessor.wallAiAvailable,
                            label = { Text(if (wallAiMode) "Wall AI" else "Plane mode") },
                            leadingIcon = if (wallAiMode) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { showControls = !showControls },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Toggle color controls",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            if (state.selectedProductName != null && state.selectedProductCode != null) {
                Surface(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 12.dp, end = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                ) {
                    Text(
                        text = "${state.selectedBrandName.orEmpty()} • ${state.selectedProductName} (${state.selectedProductCode})",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (wallAiMode && wallSegmentation == null) {
                Surface(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 102.dp, start = 12.dp, end = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Text(
                        text = "Wall AI is scanning... move phone slowly.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val mask = wallSegmentation
                if (wallAiMode && mask != null && wallMaskOverlay != null) {
                    drawImage(image = wallMaskOverlay!!)
                }
                if (state.showOutlines) {
                    drawCircle(
                        color = state.selectedColor.copy(alpha = 0.18f),
                        radius = size.minDimension * 0.46f,
                        center = center,
                        blendMode = BlendMode.Screen,
                    )
                }
                if (state.showOutlines && state.planeCount > 0) {
                    drawRoundRect(
                        color = state.selectedColor.copy(alpha = 0.85f),
                        topLeft = Offset(40f, 220f),
                        size = androidx.compose.ui.geometry.Size(size.width - 80f, size.height - 360f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f),
                        style = Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 18f), dashPhase),
                        ),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.coachingVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val handScale by animateFloatAsState(
                            targetValue = if (state.coachingVisible) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "handScale",
                        )
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).scale(handScale),
                        )
                        Text("Move phone slowly side to side", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("FPS: $fps", style = MaterialTheme.typography.labelSmall)
                        Text("Infer: ${inferenceMs}ms", style = MaterialTheme.typography.labelSmall)
                        Text("Mem: ${memMb}MB", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (showControls) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = 120.dp, max = 420.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        )
                        Text(
                            "Pick a swatch or enter a hex color to repaint surfaces instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(premiumPalette) { index, color ->
                                val isSelected = index == selectedSwatch
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.08f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "swatchScale",
                                )
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(scale)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape,
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                selectedSwatch = index
                                                viewModel.setColor(color)
                                                showControls = false
                                            },
                                            onLongClick = { popupSwatch = color },
                                        )
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(PaintSurfaceFilter.entries.toList()) { _, filter ->
                                FilterChip(
                                    selected = state.selectedFilter == filter,
                                    onClick = { viewModel.setFilter(filter) },
                                    label = { Text(filter.label) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.selectedColorHex,
                            onValueChange = { hex ->
                                val clean = hex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6)
                                viewModel.setHex(clean)
                                if (clean.length == 6) {
                                    runCatching { Color(android.graphics.Color.parseColor("#$clean")) }.getOrNull()?.let(viewModel::setColor)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Hex color") },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .background(state.selectedColor, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            },
                            singleLine = true,
                        )
                        ColorOpacitySlider(color = state.selectedColor, value = state.tintOpacity, onValueChange = viewModel::setOpacity)
                        TextButton(onClick = onOpenBrandPicker) { Text("Open brand picker") }
                        TextButton(onClick = { showPaletteLoader = true }) { Text("Load palette") }
                        TextButton(onClick = { showResetDialog = true }) {
                            Text("Reset all colors", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showPaletteLoader) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteLoader = false },
            sheetState = paletteSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Saved palettes", style = MaterialTheme.typography.titleMedium)
                if (savedPalettes.isEmpty()) {
                    Text("No palettes yet. Navigate away once to auto-save current color.")
                } else {
                    savedPalettes.take(20).forEach { palette ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.applyPalette(palette)
                                    showPaletteLoader = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(palette.name)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                palette.colors.take(3).forEach { color ->
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .background(color, CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all colors?") },
            text = { Text("This will clear current color, brand selection, and finish settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllColors()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
    if (popupSwatch != null) {
        Popup(onDismissRequest = { popupSwatch = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(120.dp).background(popupSwatch!!, RoundedCornerShape(12.dp)))
                    Text("#${String.format("%06X", popupSwatch!!.toArgb() and 0xFFFFFF)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            viewModel.setColor(popupSwatch!!)
                            showControls = false
                            popupSwatch = null
                        }) { Text("Use in AR") }
                        TextButton(onClick = {
                            viewModel.autoSaveCurrentPalette()
                            popupSwatch = null
                        }) { Text("Save to palette") }
                    }
                }
            }
        }
    }
}

private fun buildWallMaskBitmap(
    segmentation: com.arhomedesign.homedesign.photo.SegmentationResult,
    tintColor: Color,
    opacity: Float,
): android.graphics.Bitmap {
    val width = segmentation.width
    val height = segmentation.height
    val out = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val px = IntArray(width * height)
    val alpha = (opacity.coerceIn(0.2f, 0.85f) * 255f).toInt()
    val rgb = tintColor.toArgb() and 0x00FFFFFF
    for (i in px.indices) {
        val cls = segmentation.maskArray[i]
        px[i] = if (cls == 1) (alpha shl 24) or rgb else 0x00000000
    }
    out.setPixels(px, 0, width, 0, 0, width, height)
    return out
}

@Composable
private fun ColorOpacitySlider(color: Color, value: Float, onValueChange: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 1f))),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0.08f..0.88f)
    }
}
