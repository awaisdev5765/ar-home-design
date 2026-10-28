package com.arhomedesign.core.di

import android.content.Context
import androidx.room.Room
import com.arhomedesign.homedesign.data.local.HomeDesignDao
import com.arhomedesign.homedesign.data.local.HomeDesignDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeDesignDataModule {
    @Provides
    @Singleton
    fun provideHomeDesignDatabase(
        @ApplicationContext context: Context,
    ): HomeDesignDatabase = Room.databaseBuilder(
        context,
        HomeDesignDatabase::class.java,
        "home_design.db",
    ).addMigrations(HomeDesignDatabase.MIGRATION_1_2).build()

    @Provides
    fun provideHomeDesignDao(database: HomeDesignDatabase): HomeDesignDao = database.homeDesignDao()
}
