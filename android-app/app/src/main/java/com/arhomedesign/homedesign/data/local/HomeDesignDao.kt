package com.arhomedesign.homedesign.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDesignDao {
    @Query("SELECT * FROM saved_palettes ORDER BY isFavorite DESC, timestamp DESC")
    fun observeSavedPalettes(): Flow<List<SavedPaletteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPalette(palette: SavedPaletteEntity): Long

    @Query("DELETE FROM saved_palettes WHERE id = :paletteId")
    suspend fun deleteSavedPaletteById(paletteId: Long)

    @Query("UPDATE saved_palettes SET isFavorite = :favorite WHERE id = :paletteId")
    suspend fun setFavorite(paletteId: Long, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecolorSession(session: RecolorSessionEntity): Long
}
