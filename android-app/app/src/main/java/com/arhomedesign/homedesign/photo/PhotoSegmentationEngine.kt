package com.arhomedesign.homedesign.photo

import android.graphics.Bitmap
import android.util.Log
import kotlin.math.abs

interface PhotoSegmentationEngine {
    suspend fun segment(bitmap: Bitmap): SegmentationResult
}

/**
 * Lightweight fallback segmentation with 5 semantic regions.
 * It keeps the pipeline production-ready while a trained 5-class TFLite model is swapped in.
 */
class HeuristicPhotoSegmentationEngine : PhotoSegmentationEngine {
    override suspend fun segment(bitmap: Bitmap): SegmentationResult {
        val width = 257
        val height = 257
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val px = IntArray(width * height)
        scaled.getPixels(px, 0, width, 0, 0, width, height)
        scaled.recycle()

        val mask = IntArray(px.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val c = px[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val luma = (r * 0.299f + g * 0.587f + b * 0.114f)
                val sat = colorSaturation(r, g, b)
                val centerDist = abs(x - width / 2) / (width / 2f)

                mask[i] = when {
                    luma > 215f && b > r && y < height * 0.65f -> RegionLabel.WINDOW.id
                    y < height * 0.28f -> RegionLabel.CEILING.id
                    y > height * 0.72f -> RegionLabel.FLOOR.id
                    sat > 0.32f && centerDist < 0.75f && y in (height * 0.36f).toInt()..(height * 0.84f).toInt() ->
                        RegionLabel.SOFA.id
                    else -> RegionLabel.WALL.id
                }
            }
        }
        return SegmentationResult(
            maskArray = mask,
            width = width,
            height = height,
            detectedClasses = mask.asSequence().mapNotNull(RegionLabel::fromMaskClass).toSet(),
        )
    }
}

class CompositePhotoSegmentationEngine(
    private val modelRunner: SegmentationModelRunner,
    private val heuristic: HeuristicPhotoSegmentationEngine = HeuristicPhotoSegmentationEngine(),
) : PhotoSegmentationEngine {
    override suspend fun segment(bitmap: Bitmap): SegmentationResult {
        val heuristicResult = heuristic.segment(bitmap)
        val modelResult = runCatching { modelRunner.segment(bitmap) }
            .onFailure { Log.w("PhotoSegmentation", "Model segmentation failed, falling back.", it) }
            .getOrNull()
            ?: return heuristicResult

        // Model provides Background/Wall/Ceiling (0/1/2). Fill Floor/Sofa/Window from heuristic.
        val merged = modelResult.maskArray.copyOf()
        for (i in merged.indices) {
            val modelClass = merged[i]
            if (modelClass == 0) {
                merged[i] = heuristicResult.maskArray.getOrElse(i) { 0 }
            } else if (modelClass == 2) {
                merged[i] = RegionLabel.CEILING.id
            } else {
                merged[i] = RegionLabel.WALL.id
            }
        }
        return SegmentationResult(
            maskArray = merged,
            width = modelResult.width,
            height = modelResult.height,
            detectedClasses = merged.asSequence().mapNotNull(RegionLabel::fromMaskClass).toSet(),
        )
    }
}

private fun colorSaturation(r: Int, g: Int, b: Int): Float {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    if (max == 0f) return 0f
    return (max - min) / max
}
