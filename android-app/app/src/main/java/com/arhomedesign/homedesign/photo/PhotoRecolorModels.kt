package com.arhomedesign.homedesign.photo

import androidx.compose.ui.graphics.Color

enum class RegionLabel(val id: Int, val displayName: String, val defaultColor: Color) {
    WALL(1, "Wall", Color(0xFFD9C7AA)),
    FLOOR(2, "Floor", Color(0xFF8C6D4F)),
    CEILING(3, "Ceiling", Color(0xFFF2EEE8)),
    SOFA(4, "Sofa", Color(0xFF7E9BB8)),
    WINDOW(5, "Window", Color(0xFF99C7E8));

    companion object {
        val ordered = entries.sortedBy { it.id }
        fun fromId(id: Int): RegionLabel = ordered.firstOrNull { it.id == id } ?: WALL
        fun fromMaskClass(maskClass: Int): RegionLabel? = ordered.firstOrNull { it.id == maskClass }
    }
}

data class SegmentationResult(
    val maskArray: IntArray,
    val width: Int,
    val height: Int,
    val detectedClasses: Set<RegionLabel>,
    // Backward compatibility aliases used by current compositor path.
    val maskWidth: Int = width,
    val maskHeight: Int = height,
    val labels: IntArray = maskArray,
)
