package com.arhomedesign.paintpreview.ml

import android.graphics.ImageFormat
import android.media.Image
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts YUV_420_888 [Image] to interleaved RGB bytes with a **center square crop**
 * resized to [outW]×[outH] (nearest-neighbor). Matches typical square DeepLab inputs.
 */
object YuvToRgbCenterCrop {

    fun toRgb(image: Image, outW: Int, outH: Int): ByteBuffer {
        require(image.format == ImageFormat.YUV_420_888) { "Expected YUV_420_888" }
        val w = image.width
        val h = image.height
        val side = minOf(w, h)
        val dx = (w - side) / 2
        val dy = (h - side) / 2

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer.duplicate()
        val uBuf = uPlane.buffer.duplicate()
        val vBuf = vPlane.buffer.duplicate()

        val yRow = yPlane.rowStride
        val yPix = yPlane.pixelStride
        val uRow = uPlane.rowStride
        val uPix = uPlane.pixelStride
        val vRow = vPlane.rowStride
        val vPix = vPlane.pixelStride

        val out = ByteBuffer.allocateDirect(outW * outH * 3).order(ByteOrder.nativeOrder())
        for (j in 0 until outH) {
            for (i in 0 until outW) {
                val sx = dx + (i * side) / outW
                val sy = dy + (j * side) / outH
                val y = yBuf.get(sy * yRow + sx * yPix).toInt() and 0xff
                val uvX = sx / 2
                val uvY = sy / 2
                val u = uBuf.get(uvY * uRow + uvX * uPix).toInt() and 0xff
                val v = vBuf.get(uvY * vRow + uvX * vPix).toInt() and 0xff

                val c = y - 16
                val d = u - 128
                val e = v - 128
                var r = (298 * c + 409 * e + 128) shr 8
                var g = (298 * c - 100 * d - 208 * e + 128) shr 8
                var b = (298 * c + 516 * d + 128) shr 8
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)
                out.put(r.toByte())
                out.put(g.toByte())
                out.put(b.toByte())
            }
        }
        out.rewind()
        return out
    }
}
