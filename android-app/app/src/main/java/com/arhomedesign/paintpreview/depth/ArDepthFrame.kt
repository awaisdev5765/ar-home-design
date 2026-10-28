package com.arhomedesign.paintpreview.depth

import android.media.Image
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** CPU copy of ARCore depth (millimeters per pixel, row-major). */
data class ArDepthFrame(val width: Int, val height: Int, val millimeters: ShortArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArDepthFrame) return false
        if (width != other.width || height != other.height) return false
        return millimeters.contentEquals(other.millimeters)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + millimeters.contentHashCode()
        return result
    }
}

object ArDepthImageCodec {

    /**
     * Copies plane 0 of [Image] from [Frame.acquireDepthImage16Bits] into a dense [ShortArray] (mm).
     */
    fun copyDepthPlaneMm(image: Image): ShortArray? {
        val plane = image.planes.getOrNull(0) ?: return null
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0) return null
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buf = plane.buffer.duplicate().order(ByteOrder.nativeOrder())
        val out = ShortArray(w * h)
        var i = 0
        for (y in 0 until h) {
            var offset = y * rowStride
            for (x in 0 until w) {
                out[i++] = buf.getShort(offset)
                offset += pixelStride
            }
        }
        return out
    }

    /**
     * Packs depth into one byte / pixel for [GLES20.GL_UNSIGNED_BYTE] luminance texture.
     * Zero depth stays 0; valid range mapped up to [maxMmForFullScale] mm.
     */
    fun packDepthToR8(millimeters: ShortArray, maxMmForFullScale: Int = 12_000): ByteBuffer {
        val bb = ByteBuffer.allocateDirect(millimeters.size).order(ByteOrder.nativeOrder())
        val cap = maxMmForFullScale.coerceAtLeast(1)
        for (s in millimeters) {
            val mm = s.toInt() and 0xffff
            val b = when {
                mm <= 0 -> 0
                else -> ((mm.coerceAtMost(cap) * 255) / cap).coerceIn(1, 255)
            }
            bb.put(b.toByte())
        }
        bb.rewind()
        return bb
    }
}
