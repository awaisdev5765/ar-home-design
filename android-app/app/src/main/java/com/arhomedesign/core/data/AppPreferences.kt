package com.arhomedesign.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ar_home_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING] ?: false }
    val notificationOptIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY] ?: false }
    val openDates: Flow<Set<String>> = context.dataStore.data.map { it[KEY_OPEN_DATES] ?: emptySet() }
    val lastOpenAt: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_OPEN] ?: 0L }

    suspend fun isOnboardingComplete(): Boolean = onboardingComplete.first()

    suspend fun setOnboardingComplete(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = done }
    }

    suspend fun setNotificationOptIn(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY] = enabled }
    }

    suspend fun markAppOpened(date: LocalDate = LocalDate.now()) {
        val key = date.toString()
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_OPEN_DATES] ?: emptySet()
            prefs[KEY_OPEN_DATES] = (current + key).takeLast(14).toSet()
            prefs[KEY_LAST_OPEN] = System.currentTimeMillis()
        }
    }

    companion object {
        val KEY_ONBOARDING: Preferences.Key<Boolean> = booleanPreferencesKey("onboarding_complete")
        val KEY_NOTIFY: Preferences.Key<Boolean> = booleanPreferencesKey("notify_opt_in")
        val KEY_OPEN_DATES: Preferences.Key<Set<String>> = stringSetPreferencesKey("open_dates")
        val KEY_LAST_OPEN: Preferences.Key<Long> = longPreferencesKey("last_open")
    }
}

private fun <T> Set<T>.takeLast(max: Int): Set<T> {
    if (size <= max) return this
    return toList().takeLast(max).toSet()
}
