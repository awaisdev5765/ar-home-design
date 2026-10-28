package com.arhomedesign.paintpreview.depth

import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Estimates a **reference wall depth** (mm) from ARCore depth + segmentation mask so foreground
 * objects can be excluded in the shader.
 */
object WallDepthEstimator {

    fun medianWallDepthMm(
        depthMm: ShortArray,
        depthW: Int,
        depthH: Int,
        mask: ByteBuffer,
        maskW: Int,
        maskH: Int,
        imageW: Int,
        imageH: Int,
    ): Float {
        if (depthW <= 0 || depthH <= 0 || maskW <= 0 || maskH <= 0) return 0f
        val side = min(imageW, imageH)
        if (side <= 0) return 0f
        val cdx = (imageW - side) / 2
        val cdy = (imageH - side) / 2
        val samples = FloatArray(512)
        var n = 0
        for (dy in 0 until depthH step 2) {
            for (dx in 0 until depthW step 2) {
                val ix = ((dx + 0.5f) / depthW * imageW).toInt().coerceIn(0, imageW - 1)
                val iy = ((dy + 0.5f) / depthH * imageH).toInt().coerceIn(0, imageH - 1)
                val mx = ((ix - cdx) * maskW) / side
                val my = ((iy - cdy) * maskH) / side
                if (mx < 0 || mx >= maskW || my < 0 || my >= maskH) continue
                val mi = my * maskW + mx
                if (mi < 0 || mi >= mask.capacity()) continue
                if ((mask.get(mi).toInt() and 0xff) < 128) continue
                val zi = dy * depthW + dx
                if (zi < 0 || zi >= depthMm.size) continue
                val z = depthMm[zi].toInt() and 0xffff
                if (z < 250) continue
                if (n < samples.size) {
                    samples[n++] = z.toFloat()
                }
            }
        }
        if (n == 0) return 0f
        samples.sort(0, n)
        return samples[n / 2]
    }
}
