package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.aiApiKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalAiApiKey = compositionLocalOf { AiApiKeyPreference.default }

object AiApiKeyPreference {

    // 明文存于 DataStore；已加入 ignorePreferencesOnExportAndImport，不随设置导出
    const val default = ""

    fun put(context: Context, scope: CoroutineScope, value: String) {
        scope.launch {
            context.dataStore.put(DataStoreKey.aiApiKey, value)
        }
    }

    fun fromPreferences(preferences: Preferences) =
        preferences[DataStoreKey.keys[aiApiKey]?.key as Preferences.Key<String>] ?: default
}
