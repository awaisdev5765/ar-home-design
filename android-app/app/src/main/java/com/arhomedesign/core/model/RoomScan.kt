package com.arhomedesign.core.model

data class RoomScan(
    val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val planeCount: Int = 0,
    val anchorCount: Int = 0,
    val widthMeters: Double = 0.0,
    val lengthMeters: Double = 0.0,
    val heightMeters: Double = 0.0,
    val confidence: Double = 0.0,
    val status: String = "captured"
)
