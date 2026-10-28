package com.arhomedesign.core.model

data class OverlayRegion(
    val id: String = "",
    val projectId: String = "",
    val roomId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val surfaceType: String = "wall",
    val materialPreset: String = "default_paint",
    val colorHex: String = "#FFFFFF",
    val positionX: Double = 0.0,
    val positionY: Double = 0.0,
    val positionZ: Double = 0.0,
    val widthMeters: Double = 1.0,
    val heightMeters: Double = 1.0,
    val rotationDeg: Double = 0.0,
    val status: String = "active"
)
