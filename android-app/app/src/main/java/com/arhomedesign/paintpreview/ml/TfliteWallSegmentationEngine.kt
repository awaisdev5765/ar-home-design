package com.arhomedesign.paintpreview.ml

import android.content.Context
import android.media.Image
import com.arhomedesign.paintpreview.depth.WallDepthEstimator
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TensorFlow Lite semantic segmentation → wall mask (0–255). Optional [GpuDelegate].
 * Model: `assets/ml/wall_deeplab.tflite`. Without the file, [isReady] is false (use plane fallback).
 *
 * **ARCore pool:** Only **one** camera [Image] may be in-flight (worker busy). Extra frames are
 * dropped and closed immediately so ARCore never hits “too many images acquired”.
 */
class TfliteWallSegmentationEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var inputW = 257
    private var inputH = 257
    private var inputIsFloat = false
    private var outputIsLogits = false
    /** True when output tensor is NCHW [1,C,H,W]; false for NHWC [1,H,W,C]. */
    private var outputIsNchw = false
    private var numClasses = 151
    private var outputInt32 = false
    private var outputByteBuffer: ByteBuffer? = null
    /** Flat NHWC logits [H*W*C] float32 — avoids nested arrays and speeds argmax. */
    private var logitsFlatBuffer: ByteBuffer? = null
    private var logitsH = 0
    private var logitsW = 0
    private var floatInputBuffer: ByteBuffer? = null

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "wall-seg-tflite").apply { isDaemon = true }
    }

    /** True while a runnable holds an [Image] (YUV→RGB + inference). Prevents executor queue buildup. */
    private val mlWorkerBusy = AtomicBoolean(false)

    private val lock = Any()
    private var latestMask: ByteBuffer? = null
    private var latestMaskW = 0
    private var latestMaskH = 0

    /** Previous-frame wall mask for temporal blend (reduces flicker / harsh edges). */
    private var maskPrevSmooth: ByteBuffer? = null
    private var maskPrevW = 0
    private var maskPrevH = 0
    private var maskBlendScratch: ByteBuffer? = null

    @Volatile
    var latestGeneration: Long = 0L
        private set

    @Volatile
    var wallDepthMillimeters: Float = 0f
        private set

    @Volatile
    var lastCameraImageW: Int = 0
        private set

    @Volatile
    var lastCameraImageH: Int = 0
        private set

    private val depthLock = Any()
    private var pendingDepthMm: ShortArray? = null
    private var pendingDepthW: Int = 0
    private var pendingDepthH: Int = 0

    private var lastRunNs = 0L
    /** ~12 ML frames/sec — keeps preview smooth; camera still updates every frame. */
    private val minIntervalNs = 80_000_000L

    val isReady: Boolean get() = interpreter != null

    /** True while the worker holds an acquired camera [Image] (skip [Frame.acquireCameraImage] to save pool pressure). */
    val isPipelineBusy: Boolean get() = mlWorkerBusy.get()

    init {
        loadModel()
    }

    private fun loadModel() {
        runCatching {
            val raw = context.assets.open("ml/wall_deeplab.tflite").use { it.readBytes() }
            val modelBb = ByteBuffer.allocateDirect(raw.size).order(ByteOrder.nativeOrder())
            modelBb.put(raw)
            modelBb.rewind()

            val opts = Interpreter.Options().apply {
                setNumThreads(2)
                useNNAPI = false
            }
            runCatching { opts.addDelegate(GpuDelegate()) }

            interpreter = Interpreter(modelBb, opts).also { interp ->
                val inTensor = interp.getInputTensor(0)
                val shape = inTensor.shape()
                require(shape.size == 4) { "Expected NHWC input" }
                inputH = shape[1]
                inputW = shape[2]
                inputIsFloat = inTensor.dataType() == DataType.FLOAT32
                if (inputIsFloat) {
                    floatInputBuffer = ByteBuffer.allocateDirect(inputW * inputH * 3 * 4)
                        .order(ByteOrder.nativeOrder())
                }
                val outTensor = interp.getOutputTensor(0)
                val osh = outTensor.shape()
                when (osh.size) {
                    4 -> {
                        outputIsLogits = true
                        val d1 = osh[1]
                        val d2 = osh[2]
                        val d3 = osh[3]
                        // NCHW [1,C,H,W] vs NHWC [1,H,W,C]: class axis is smallest in NCHW.
                        if (d1 < d2 && d1 < d3 && d1 <= 512) {
                            outputIsNchw = true
                            numClasses = d1
                            logitsH = d2
                            logitsW = d3
                        } else {
                            outputIsNchw = false
                            logitsH = d1
                            logitsW = d2
                            numClasses = d3
                        }
                        val c = numClasses
                        logitsFlatBuffer = ByteBuffer.allocateDirect(logitsH * logitsW * c * 4)
                            .order(ByteOrder.nativeOrder())
                    }
                    3 -> {
                        outputIsLogits = false
                        val count = osh[1] * osh[2]
                        val dtype = outTensor.dataType()
                        outputInt32 = dtype == DataType.INT32
                        val bpp = if (outputInt32) 4 else 1
                        outputByteBuffer = ByteBuffer.allocateDirect(count * bpp).order(ByteOrder.nativeOrder())
                    }
                    else -> error("Unexpected output rank ${osh.size}")
                }
            }
        }.onFailure {
            interpreter = null
        }
    }

    fun setFrameMeta(width: Int, height: Int) {
        lastCameraImageW = width
        lastCameraImageH = height
    }

    fun setPendingDepth(millimeters: ShortArray?, width: Int, height: Int) {
        synchronized(depthLock) {
            pendingDepthMm = millimeters
            pendingDepthW = width
            pendingDepthH = height
        }
    }

    /**
     * Enqueues work on the ML thread. **Always** takes ownership of [image]: it is closed after
     * processing, or immediately if throttled / no interpreter.
     *
     * @param cameraImageW [Image.getWidth] for this frame (stored for depth/mask alignment).
     */
    fun scheduleFrame(image: Image, cameraImageW: Int, cameraImageH: Int) {
        if (interpreter == null) {
            image.close()
            return
        }
        val now = System.nanoTime()
        if (now - lastRunNs < minIntervalNs) {
            image.close()
            return
        }
        if (!mlWorkerBusy.compareAndSet(false, true)) {
            image.close()
            return
        }
        lastRunNs = now

        try {
            executor.execute {
                try {
                    lastCameraImageW = cameraImageW
                    lastCameraImageH = cameraImageH
                    val rgb = try {
                        YuvToRgbCenterCrop.toRgb(image, inputW, inputH)
                    } catch (_: Throwable) {
                        return@execute
                    }
                    val input = if (inputIsFloat) rgbToFloatNhwc(rgb) else rgb
                    runInference(input)
                } finally {
                    image.close()
                    mlWorkerBusy.set(false)
                }
            }
        } catch (_: RejectedExecutionException) {
            mlWorkerBusy.set(false)
            image.close()
        }
    }

    private fun rgbToFloatNhwc(rgb: ByteBuffer): ByteBuffer {
        val fb = floatInputBuffer ?: return rgb
        fb.clear()
        val floats = fb.asFloatBuffer()
        rgb.rewind()
        val n = inputW * inputH * 3
        for (i in 0 until n) {
            floats.put((rgb.get().toInt() and 0xff) / 255f)
        }
        fb.rewind()
        return fb
    }

    private fun runInference(input: ByteBuffer) {
        val interp = interpreter ?: return
        val h = logitsH
        val w = logitsW
        val c = numClasses
        try {
            input.rewind()
            if (outputIsLogits) {
                val lb = logitsFlatBuffer ?: return
                lb.clear()
                interp.run(input, lb)
                lb.rewind()
                val fb = lb.asFloatBuffer()
                val mask = ByteBuffer.allocateDirect(h * w).order(ByteOrder.nativeOrder())
                if (outputIsNchw) {
                    val hw = h * w
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val planeOffset = y * w + x
                            var best = 0
                            var bestV = fb.get(planeOffset)
                            for (k in 1 until c) {
                                val v = fb.get(k * hw + planeOffset)
                                if (v > bestV) {
                                    bestV = v
                                    best = k
                                }
                            }
                            val wall = WallSegmentationLabels.isWallClass(best)
                            mask.put(if (wall) 255.toByte() else 0.toByte())
                        }
                    }
                } else {
                    val rowStride = w * c
                    for (y in 0 until h) {
                        var rowBase = y * rowStride
                        for (x in 0 until w) {
                            var best = 0
                            var bestV = fb.get(rowBase)
                            for (k in 1 until c) {
                                val v = fb.get(rowBase + k)
                                if (v > bestV) {
                                    bestV = v
                                    best = k
                                }
                            }
                            rowBase += c
                            val wall = WallSegmentationLabels.isWallClass(best)
                            mask.put(if (wall) 255.toByte() else 0.toByte())
                        }
                    }
                }
                mask.rewind()
                finishMask(mask, w, h)
            } else {
                val ob = outputByteBuffer ?: return
                ob.clear()
                interp.run(input, ob)
                val oh = interp.getOutputTensor(0).shape()[1]
                val ow = interp.getOutputTensor(0).shape()[2]
                val mask = ByteBuffer.allocateDirect(oh * ow).order(ByteOrder.nativeOrder())
                if (outputInt32) {
                    ob.rewind()
                    val ib = ob.asIntBuffer()
                    for (i in 0 until oh * ow) {
                        val cid = ib.get()
                        val wall = WallSegmentationLabels.isWallClass(cid)
                        mask.put(if (wall) 255.toByte() else 0.toByte())
                    }
                } else {
                    ob.rewind()
                    for (i in 0 until oh * ow) {
                        val cid = ob.get().toInt() and 0xff
                        val wall = WallSegmentationLabels.isWallClass(cid)
                        mask.put(if (wall) 255.toByte() else 0.toByte())
                    }
                }
                mask.rewind()
                finishMask(mask, ow, oh)
            }
        } catch (_: Throwable) {
            // drop frame
        }
    }

    private fun finishMask(mask: ByteBuffer, w: Int, h: Int) {
        temporalBlendWallMask(mask, w, h)
        val iw = lastCameraImageW
        val ih = lastCameraImageH
        val depthMm: ShortArray?
        val dw: Int
        val dh: Int
        synchronized(depthLock) {
            depthMm = pendingDepthMm
            dw = pendingDepthW
            dh = pendingDepthH
        }
        val wallMm = if (depthMm != null && iw > 0 && ih > 0 && dw > 0 && dh > 0) {
            WallDepthEstimator.medianWallDepthMm(
                depthMm,
                dw,
                dh,
                mask.duplicate(),
                w,
                h,
                iw,
                ih,
            )
        } else {
            0f
        }
        wallDepthMillimeters = wallMm
        publishMask(mask, w, h)
    }

    /**
     * EMA-style blend with the previous mask (same resolution). First frame seeds history only.
     */
    private fun temporalBlendWallMask(mask: ByteBuffer, w: Int, h: Int) {
        val n = w * h
        if (n <= 0) return
        var prev = maskPrevSmooth
        if (prev == null || maskPrevW != w || maskPrevH != h || prev.capacity() < n) {
            prev = ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder())
            maskPrevSmooth = prev
            mask.rewind()
            prev.clear()
            prev.put(mask)
            prev.rewind()
            mask.rewind()
            maskPrevW = w
            maskPrevH = h
            return
        }
        var scratch = maskBlendScratch
        if (scratch == null || scratch.capacity() < n) {
            scratch = ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder())
            maskBlendScratch = scratch
        }
        scratch.clear()
        mask.rewind()
        prev.rewind()
        for (i in 0 until n) {
            val neu = mask.get().toInt() and 0xff
            val pr = prev.get().toInt() and 0xff
            scratch.put(((neu * 58 + pr * 42) / 100).coerceIn(0, 255).toByte())
        }
        scratch.rewind()
        mask.clear()
        mask.put(scratch)
        mask.rewind()
        prev.clear()
        prev.put(mask.duplicate())
        prev.rewind()
    }

    private fun publishMask(buf: ByteBuffer, w: Int, h: Int) {
        synchronized(lock) {
            latestMask = buf
            latestMaskW = w
            latestMaskH = h
            latestGeneration++
        }
    }

    fun consumeMaskForUpload(): Triple<ByteBuffer, Int, Int>? {
        synchronized(lock) {
            val m = latestMask ?: return null
            val w = latestMaskW
            val h = latestMaskH
            val copy = ByteBuffer.allocateDirect(m.remaining()).order(ByteOrder.nativeOrder())
            copy.put(m.duplicate())
            copy.rewind()
            return Triple(copy, w, h)
        }
    }

    fun shutdown() {
        interpreter?.close()
        interpreter = null
        executor.shutdownNow()
        mlWorkerBusy.set(false)
        maskPrevSmooth = null
        maskBlendScratch = null
        maskPrevW = 0
        maskPrevH = 0
    }
}
