package com.arhomedesign.homedesign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedPaletteEntity::class, RecolorSessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class HomeDesignDatabase : RoomDatabase() {
    abstract fun homeDesignDao(): HomeDesignDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_palettes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
