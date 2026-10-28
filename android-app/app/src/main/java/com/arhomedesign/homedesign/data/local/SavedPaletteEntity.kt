package com.arhomedesign.homedesign.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_palettes")
data class SavedPaletteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorsJson: String,
    val timestamp: Long,
    val isFavorite: Boolean = false,
)
