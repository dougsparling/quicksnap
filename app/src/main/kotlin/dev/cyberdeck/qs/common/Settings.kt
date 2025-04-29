package dev.cyberdeck.qs.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class Settings private constructor(
    private val context: Context
) {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    fun wideAngle() = context.dataStore.data.map { it[WIDE_ANGLE] == true }

    suspend fun toggleWideAngle() =
        context.dataStore.edit { it[WIDE_ANGLE] = it[WIDE_ANGLE] != true }

    companion object {
        val WIDE_ANGLE = booleanPreferencesKey("wideAngle")

        // datastore has to be a singleton but it complains if I retain a reference, so idiotic
        @SuppressLint("StaticFieldLeak")
        private var instance: Settings? = null

        fun get(ctx: Context): Settings {
            instance?.let { return it }
            instance = Settings(ctx.applicationContext)
            return instance!!
        }
    }
}