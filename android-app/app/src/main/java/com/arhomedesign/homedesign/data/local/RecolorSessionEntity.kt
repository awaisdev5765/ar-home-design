package com.arhomedesign.homedesign.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recolor_sessions")
data class RecolorSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val thumbnailUri: String,
    val surfaceColorsJson: String,
    val mode: String,
    val timestamp: Long,
)
