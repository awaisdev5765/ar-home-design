package com.arhomedesign.paintpreview.ml

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.arhomedesign.homedesign.photo.CompositePhotoSegmentationEngine
import com.arhomedesign.homedesign.photo.HeuristicPhotoSegmentationEngine
import com.arhomedesign.homedesign.photo.SegmentationModelRunner
import com.arhomedesign.homedesign.photo.SegmentationResult
import com.google.ar.core.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class ARSegmentationProcessor(context: Context) {
    private val modelRunner = SegmentationModelRunner(context)
    private val engine = CompositePhotoSegmentationEngine(modelRunner, HeuristicPhotoSegmentationEngine())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var frameCounter = 0
    private var runningJob: Job? = null
    private var active = true
    private var lastThumb: IntArray? = null

    private val _segmentation = MutableStateFlow<SegmentationResult?>(null)
    val segmentation: StateFlow<SegmentationResult?> = _segmentation.asStateFlow()
    private val _inferenceMs = MutableStateFlow(0L)
    val inferenceMs: StateFlow<Long> = _inferenceMs.asStateFlow()

    val wallAiAvailable: Boolean
        get() = modelRunner.isModelAvailable

    fun onFrame(frame: Frame) {
        if (!active) return
        frameCounter++
        if (frameCounter % 8 != 0) return
        if (runningJob?.isActive == true) return
        runningJob = scope.launch {
            val image = runCatching { frame.acquireCameraImage() }.getOrNull() ?: return@launch
            val bitmap = runCatching { image.toBitmap() }
                .onFailure { Log.w("ARSegmentation", "YUV conversion failed", it) }
                .getOrNull()
            image.close()
            if (bitmap == null) return@launch
            val thumb = bitmap.thumbnailSignature(16)
            val last = lastThumb
            if (last != null && thumbnailDelta(last, thumb) < 0.03f) {
                bitmap.recycle()
                return@launch
            }
            lastThumb = thumb
            val start = System.currentTimeMillis()
            val result = runCatching { engine.segment(bitmap) }
                .onFailure { Log.w("ARSegmentation", "Segmentation failed", it) }
                .getOrNull()
            bitmap.recycle()
            if (result != null) {
                _segmentation.value = result
                _inferenceMs.value = System.currentTimeMillis() - start
            }
        }
    }

    fun setActive(enabled: Boolean) {
        active = enabled
    }

    fun stop() {
        runningJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }
}

private fun Image.toBitmap(): Bitmap {
    val out = 513
    val rgb = YuvToRgbCenterCrop.toRgb(this, out, out)
    val pixels = IntArray(out * out)
    for (i in pixels.indices) {
        val r = rgb.get().toInt() and 0xFF
        val g = rgb.get().toInt() and 0xFF
        val b = rgb.get().toInt() and 0xFF
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    return Bitmap.createBitmap(pixels, out, out, Bitmap.Config.ARGB_8888)
}

private fun Bitmap.thumbnailSignature(size: Int): IntArray {
    val small = Bitmap.createScaledBitmap(this, size, size, true)
    val px = IntArray(size * size)
    small.getPixels(px, 0, size, 0, 0, size, size)
    small.recycle()
    return px
}

private fun thumbnailDelta(a: IntArray, b: IntArray): Float {
    if (a.size != b.size) return 1f
    var acc = 0f
    for (i in a.indices) {
        val ar = (a[i] shr 16) and 0xFF
        val ag = (a[i] shr 8) and 0xFF
        val ab = a[i] and 0xFF
        val br = (b[i] shr 16) and 0xFF
        val bg = (b[i] shr 8) and 0xFF
        val bb = b[i] and 0xFF
        acc += (abs(ar - br) + abs(ag - bg) + abs(ab - bb)) / (255f * 3f)
    }
    return acc / a.size
}
