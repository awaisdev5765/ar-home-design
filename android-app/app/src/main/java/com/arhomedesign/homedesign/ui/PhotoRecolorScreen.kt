package com.arhomedesign.homedesign.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.ShareCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.arhomedesign.homedesign.photo.PhotoRecolorViewModel
import com.arhomedesign.homedesign.photo.RegionLabel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoRecolorScreen(viewModel: PhotoRecolorViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var pickerSheetOpen by remember { mutableStateOf(false) }
    var makeoverMode by remember { mutableStateOf(false) }
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraPermissionGranted = it
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            decodeBitmap(context, uri)?.let(viewModel::loadImage)
        }
    }

    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offsetX += pan.x
        offsetY += pan.y
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text("Photo Recolor") },
                actions = {
                    IconButton(onClick = { makeoverMode = !makeoverMode }) {
                        Icon(Icons.Rounded.ViewCarousel, contentDescription = "Room Makeover mode")
                    }
                    IconButton(onClick = {
                        val source = uiState.source ?: return@IconButton
                        val preview = uiState.preview ?: source
                        val shareBitmap = if (makeoverMode) {
                            composeMakeoverBitmap(source, preview)
                        } else {
                            composeShareCardBitmap(source, preview, uiState.regionColors.values.toList())
                        }
                        val uri = saveBitmapToMediaStore(context, shareBitmap) ?: return@IconButton
                        ShareCompat.IntentBuilder(context)
                            .setType("image/jpeg")
                            .setStream(uri)
                            .setChooserTitle("Share AR Home Design")
                            .startChooser()
                        shareBitmap.recycle()
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "Import",
                        modifier = Modifier.padding(end = 12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (cameraPermissionGranted) return@Button
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    enabled = !cameraPermissionGranted,
                ) { Icon(Icons.Rounded.Camera, null); Text("Enable Camera") }
                Button(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                ) { Icon(Icons.Rounded.Image, null); Text("Import") }
            }

            if (uiState.source == null) {
                CameraCaptureBox(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    enabled = cameraPermissionGranted,
                    lifecycleOwner = lifecycleOwner,
                    onImageCaptureReady = { imageCapture = it },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .transformable(transformState),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = uiState.preview ?: uiState.source,
                        contentDescription = "Recolor preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    )
                    if (uiState.loading) {
                        ShimmerLoading(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RegionLabel.ordered) { region ->
                    FilterChip(
                        selected = uiState.selectedRegion == region || uiState.isolateClass == region,
                        onClick = {
                            viewModel.selectRegion(region)
                            if (uiState.segmentation?.detectedClasses?.contains(region) == true) {
                                viewModel.setIsolateClass(
                                    if (uiState.isolateClass == region) null else region
                                )
                            } else {
                                pickerSheetOpen = true
                            }
                        },
                        label = { Text(region.displayName) },
                        leadingIcon = {
                            Box(
                                Modifier.size(14.dp).clip(CircleShape)
                                    .background(uiState.regionColors[region] ?: region.defaultColor)
                            )
                        },
                    )
                }
            }
            if (!uiState.segmentation?.detectedClasses.isNullOrEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.segmentation?.detectedClasses?.sortedBy { it.id } ?: emptyList()) { region ->
                        FilterChip(
                            selected = uiState.isolateClass == region,
                            onClick = {
                                viewModel.setIsolateClass(if (uiState.isolateClass == region) null else region)
                            },
                            label = { Text("AI ${region.displayName}") },
                            leadingIcon = {
                                Box(Modifier.size(12.dp).background(region.defaultColor, CircleShape))
                            },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Blend")
                Slider(
                    value = uiState.opacity,
                    onValueChange = viewModel::setOpacity,
                    valueRange = 0.3f..0.9f,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        imageCapture?.let { capture ->
                            captureFromCamera(context, capture) { bitmap ->
                                if (bitmap != null) viewModel.loadImage(bitmap)
                            }
                        }
                    },
                    enabled = uiState.source == null && cameraPermissionGranted,
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Rounded.Camera, null); Text("Capture") }

                Button(
                    onClick = {
                        val preview = uiState.preview ?: return@Button
                        val uri = saveBitmapToMediaStore(context, preview)
                        scope.launch {
                            val result = snackbars.showSnackbar("Saved to gallery", actionLabel = if (uri != null) "View" else null)
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && uri != null) {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "image/*")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }
                        }
                    },
                    enabled = uiState.preview != null,
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Rounded.Save, null); Text("Save") }

                Button(
                    onClick = {
                        val preview = uiState.preview ?: return@Button
                        val uri = saveBitmapToMediaStore(context, preview) ?: return@Button
                        ShareCompat.IntentBuilder(context)
                            .setType("image/*")
                            .setStream(uri)
                            .setChooserTitle("Share recolored image")
                            .startChooser()
                    },
                    enabled = uiState.preview != null,
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Rounded.Share, null); Text("Share") }
            }

            if (uiState.errorMessage != null) {
                AssistChip(onClick = viewModel::clearError, label = { Text(uiState.errorMessage ?: "Error") })
            }
        }
    }

    if (pickerSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerSheetOpen = false },
            sheetState = pickerSheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Color: ${uiState.selectedRegion.displayName}", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(brandMiniColors) { c ->
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(c)
                                .clickable {
                                    viewModel.setColorForSelected(c)
                                    pickerSheetOpen = false
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureBox(
    modifier: Modifier,
    enabled: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!enabled) {
            Text("Enable camera permission to capture", modifier = Modifier.align(Alignment.Center))
            return
        }
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val capture = ImageCapture.Builder().build()
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    onImageCaptureReady(capture)
                }, ContextCompat.getMainExecutor(context))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ShimmerLoading(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.35f)) {
        drawRect(color = Color.Black)
    }
}

private val brandMiniColors = listOf(
    Color(0xFFF9F9F6), Color(0xFFE8E0D5), Color(0xFF8FAE8B), Color(0xFF7EB6D9),
    Color(0xFF4A6FA5), Color(0xFFC4705A), Color(0xFF9B2335), Color(0xFF5C6670),
)

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}.getOrNull()

private fun captureFromCamera(context: Context, imageCapture: ImageCapture, onResult: (Bitmap?) -> Unit) {
    val photoFile = File.createTempFile("photo_recolor_", ".jpg", context.cacheDir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(photoFile).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onResult(decodeBitmap(context, uri))
            }

            override fun onError(exception: ImageCaptureException) {
                onResult(null)
            }
        },
    )
}

private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? = runCatching {
    val now = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "ARHomeDesign_$now.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ARHomeDesign")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    context.contentResolver.openOutputStream(uri)?.use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
    }
    uri
}.getOrNull()

private fun composeShareCardBitmap(source: Bitmap, recolored: Bitmap, swatches: List<Color>): Bitmap {
    val width = recolored.width
    val strip = 140
    val out = Bitmap.createBitmap(width, recolored.height + strip, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawBitmap(recolored, 0f, 0f, null)
    val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; alpha = 170 }
    canvas.drawRect(0f, recolored.height.toFloat(), width.toFloat(), out.height.toFloat(), paint)
    val sw = 40f
    val gap = 16f
    var x = 24f
    val y = recolored.height + 44f
    swatches.take(6).forEach {
        val p = android.graphics.Paint().apply { color = it.toArgb() }
        canvas.drawRoundRect(android.graphics.RectF(x, y, x + sw, y + sw), 12f, 12f, p)
        x += sw + gap
    }
    val text = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isAntiAlias = true
    }
    canvas.drawText("AR Home Design", width - 260f, out.height - 24f, text)
    return out
}

private fun composeMakeoverBitmap(before: Bitmap, after: Bitmap): Bitmap {
    val outW = 1920
    val outH = 1080
    val half = outW / 2
    val beforeScaled = Bitmap.createScaledBitmap(before, half, outH, true)
    val afterScaled = Bitmap.createScaledBitmap(after, half, outH, true)
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawBitmap(beforeScaled, 0f, 0f, null)
    canvas.drawBitmap(afterScaled, half.toFloat(), 0f, null)
    val text = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 36f; isAntiAlias = true }
    canvas.drawText("Before", 40f, 52f, text)
    canvas.drawText("After", (half + 40).toFloat(), 52f, text)
    beforeScaled.recycle()
    afterScaled.recycle()
    return out
}
