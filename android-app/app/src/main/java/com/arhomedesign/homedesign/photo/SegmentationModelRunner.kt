package com.arhomedesign.homedesign.photo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

class SegmentationModelRunner(private val context: Context) {
    private val inputSize = 513
    private val assetName = "segmentation_model.tflite"

    private val interpreter: Interpreter? by lazy {
        runCatching { createInterpreter(assetName) }
            .recoverCatching {
                Log.w("SegmentationModelRunner", "Primary model missing. Trying TFHub fallback.", it)
                createInterpreter("deeplabv3.tflite")
            }
            .onFailure {
                Log.w("SegmentationModelRunner", "No TFLite model available. Heuristic fallback only.", it)
            }
            .getOrNull()
    }

    private val hasAnyModelAsset: Boolean by lazy {
        runCatching { context.assets.open(assetName).close(); true }
            .recoverCatching { context.assets.open("deeplabv3.tflite").close(); true }
            .getOrDefault(false)
    }
    val isModelAvailable: Boolean
        get() = hasAnyModelAsset

    suspend fun segment(bitmap: Bitmap): SegmentationResult = withContext(Dispatchers.Default) {
        val interpreter = interpreter ?: error("Segmentation model not loaded")
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = bitmapToInputBuffer(scaled)
        val output = Array(1) { Array(inputSize) { IntArray(inputSize) } }
        interpreter.run(input, output)
        scaled.recycle()

        val flat = IntArray(inputSize * inputSize)
        var k = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val raw = output[0][y][x]
                // Your model mapping: 0=Background, 1=Wall, 2=Ceiling.
                // Keep TFHub remap path as secondary fallback.
                val mapped = when (raw) {
                    0 -> 0
                    1 -> RegionLabel.WALL.id
                    2 -> RegionLabel.CEILING.id
                    else -> tfHubToAppClass(raw)
                }
                flat[k++] = mapped
            }
        }
        SegmentationResult(
            maskArray = flat,
            width = inputSize,
            height = inputSize,
            detectedClasses = flat.asSequence().mapNotNull(RegionLabel::fromMaskClass).toSet(),
        )
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte())
            buffer.put(((pixel shr 8) and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
        buffer.rewind()
        return buffer
    }

    private fun createInterpreter(name: String): Interpreter {
        val modelBytes = context.assets.open(name).use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(modelBytes)
        modelBuffer.rewind()
        val options = Interpreter.Options()
        options.setNumThreads(4)
        return Interpreter(modelBuffer, options)
    }

    private fun tfHubToAppClass(tfHubClass: Int): Int {
        // Coarse ADE20K remap as secondary fallback path.
        return when (tfHubClass) {
            0 -> 0
            1, 12, 23 -> RegionLabel.WALL.id
            3, 6 -> RegionLabel.FLOOR.id
            5 -> RegionLabel.CEILING.id
            10, 14 -> RegionLabel.SOFA.id
            8, 9 -> RegionLabel.WINDOW.id
            else -> 0
        }
    }
}
