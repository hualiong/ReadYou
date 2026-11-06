package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.aiThinking
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalAiThinking = compositionLocalOf { AiThinkingPreference.default }

/** When off, the request asks the endpoint to disable reasoning (thinking) mode. On = send nothing extra. */
class AiThinkingPreference(val value: Boolean) : Preference() {
    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(DataStoreKey.aiThinking, value)
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        AiThinkingPreference(!value).put(context, scope)

    companion object {
        val default = AiThinkingPreference(true)
        fun fromPreference(preference: Preferences): AiThinkingPreference {
            return AiThinkingPreference(
                preference[DataStoreKey.keys[aiThinking]?.key as Preferences.Key<Boolean>] ?: return default
            )
        }
    }
}
