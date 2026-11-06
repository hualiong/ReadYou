package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.aiPrompt
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalAiPrompt = compositionLocalOf { AiPromptPreference.default }

object AiPromptPreference {

    const val default =
        "Summarize the article given by the user. Write in the same language as the article. " +
            "Split the summary into 2-3 short paragraphs, each covering one aspect in the order it " +
            "appears in the article, and keep each paragraph to 1-2 sentences. Be specific and " +
            "factual. Output the summary only: no title, no preamble, no closing remark."

    fun put(context: Context, scope: CoroutineScope, value: String) {
        scope.launch {
            context.dataStore.put(DataStoreKey.aiPrompt, value)
        }
    }

    fun fromPreferences(preferences: Preferences) =
        preferences[DataStoreKey.keys[aiPrompt]?.key as Preferences.Key<String>] ?: default
}
