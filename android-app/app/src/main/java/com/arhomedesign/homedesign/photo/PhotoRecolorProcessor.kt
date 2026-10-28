package com.arhomedesign.homedesign.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object PhotoRecolorProcessor {
    fun composite(
        source: Bitmap,
        segmentation: SegmentationResult,
        colors: Map<RegionLabel, Color>,
        opacity: Float,
        isolateClass: RegionLabel? = null,
    ): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height
        val overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val overlayPixels = IntArray(width * height)
        val labelPixels = segmentation.labels
        val alpha = (opacity.coerceIn(0.3f, 0.9f) * 255f).toInt()

        for (y in 0 until height) {
            val my = ((y / height.toFloat()) * (segmentation.maskHeight - 1)).toInt()
            for (x in 0 until width) {
                val mx = ((x / width.toFloat()) * (segmentation.maskWidth - 1)).toInt()
                val label = RegionLabel.fromId(labelPixels[my * segmentation.maskWidth + mx])
                if (isolateClass != null && isolateClass != label) {
                    overlayPixels[y * width + x] = 0x00000000
                    continue
                }
                val rgb = colors[label]?.toArgb() ?: label.defaultColor.toArgb()
                overlayPixels[y * width + x] = (alpha shl 24) or (rgb and 0x00FFFFFF)
            }
        }
        overlay.setPixels(overlayPixels, 0, width, 0, 0, width, height)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawBitmap(overlay, 0f, 0f, paint)
        overlay.recycle()
        return output
    }
}
