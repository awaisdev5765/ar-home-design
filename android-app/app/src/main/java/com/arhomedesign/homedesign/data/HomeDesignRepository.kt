package com.arhomedesign.homedesign.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arhomedesign.homedesign.data.local.HomeDesignDao
import com.arhomedesign.homedesign.data.local.RecolorSessionEntity
import com.arhomedesign.homedesign.data.local.SavedPaletteEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SavedPalette(
    val id: Long,
    val name: String,
    val colors: List<Color>,
    val timestamp: Long,
    val isFavorite: Boolean,
)

@Singleton
class HomeDesignRepository @Inject constructor(
    private val dao: HomeDesignDao,
) {
    fun observeSavedPalettes(): Flow<List<SavedPalette>> =
        dao.observeSavedPalettes().mapToDomain()

    suspend fun savePalette(name: String, colors: List<Color>, isFavorite: Boolean = false): Long {
        val entity = SavedPaletteEntity(
            name = name,
            colorsJson = encodeColors(colors),
            timestamp = System.currentTimeMillis(),
            isFavorite = isFavorite,
        )
        return dao.insertSavedPalette(entity)
    }

    suspend fun deletePalette(id: Long) = dao.deleteSavedPaletteById(id)
    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun saveRecolorSession(thumbnailUri: String, surfaceColors: List<Color>, mode: String) {
        dao.insertRecolorSession(
            RecolorSessionEntity(
                thumbnailUri = thumbnailUri,
                surfaceColorsJson = encodeColors(surfaceColors),
                mode = mode,
                timestamp = System.currentTimeMillis(),
            )
        )
    }
}

private fun Flow<List<SavedPaletteEntity>>.mapToDomain(): Flow<List<SavedPalette>> =
    map { entities ->
        entities.map { entity ->
            SavedPalette(
                id = entity.id,
                name = entity.name,
                colors = decodeColors(entity.colorsJson),
                timestamp = entity.timestamp,
                isFavorite = entity.isFavorite,
            )
        }
    }

private fun encodeColors(colors: List<Color>): String =
    colors.joinToString(separator = ",") { color ->
        String.format("%06X", color.toArgb() and 0xFFFFFF)
    }

private fun decodeColors(serialized: String): List<Color> =
    serialized.split(",")
        .mapNotNull { token ->
            val clean = token.trim()
            if (clean.length != 6) return@mapNotNull null
            runCatching { Color(android.graphics.Color.parseColor("#$clean")) }.getOrNull()
        }
