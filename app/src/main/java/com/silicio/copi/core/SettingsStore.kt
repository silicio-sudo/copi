package com.silicio.copi.core

import android.content.Context
import android.content.SharedPreferences
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.RoadbookCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes persistentes. Se guardan como un único JSON en SharedPreferences para
 * que la migración de campos sea sencilla (ignoreUnknownKeys + defaults).
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("copi_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    val current: AppSettings get() = _state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_state.value)
        _state.value = updated
        prefs.edit().putString(KEY, RoadbookCodec.json.encodeToString(AppSettings.serializer(), updated)).apply()
    }

    fun replace(settings: AppSettings) {
        _state.value = settings
        prefs.edit().putString(KEY, RoadbookCodec.json.encodeToString(AppSettings.serializer(), settings)).apply()
    }

    private fun load(): AppSettings {
        val raw = prefs.getString(KEY, null) ?: return AppSettings()
        return runCatching {
            RoadbookCodec.json.decodeFromString(AppSettings.serializer(), raw)
        }.getOrDefault(AppSettings())
    }

    companion object {
        private const val KEY = "settings_json_v1"
    }
}
