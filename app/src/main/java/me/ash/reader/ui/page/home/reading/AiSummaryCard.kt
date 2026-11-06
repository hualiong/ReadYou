package me.ash.reader.ui.page.home.reading

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.ui.page.adaptive.AiSummaryState

/** Summary of the article below the metadata; toggled by the top bar button, so it has no close affordance. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiSummaryCard(state: AiSummaryState, modifier: Modifier = Modifier) {
    val text =
        when (state) {
            is AiSummaryState.Streaming -> state.text
            is AiSummaryState.Done -> state.text
            is AiSummaryState.Error -> state.message
            AiSummaryState.Idle, AiSummaryState.Thinking -> ""
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                // Tonal filled card, no border: surfaceContainerHigh reads against the
                // surface background in both light and dark (PiliNovel CommentCard recipe)
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        // Symmetric insets: the header is a text line (unlike PiliNovel's avatar row), so
        // equal top/bottom keeps the loading state centered and the full card balanced.
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    // 24dp: Material 3's default icon size, and 1.5x titleMedium's 16sp
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                val thinking = state is AiSummaryState.Thinking
                AnimatedContent(
                    targetState = thinking,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 }) togetherWith
                            (fadeOut(tween(160)) + slideOutVertically(tween(220)) { -it / 2 })
                    },
                    label = "aiSummaryTitle",
                    modifier = Modifier.weight(1f),
                ) { isThinking ->
                    Text(
                        text = stringResource(if (isThinking) R.string.ai_summary_thinking else R.string.ai_summary),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (thinking || (state is AiSummaryState.Streaming && text.isEmpty())) {
                    // The indicator only paints ~2/3 of its box; 30dp shows a ~20dp spinner.
                    LoadingIndicator(modifier = Modifier.size(30.dp))
                }
            }
            if (text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = text,
                    style =
                        if (state is AiSummaryState.Error) {
                            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F6)
@Composable
fun AiSummaryCardPrev() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AiSummaryCard(state = AiSummaryState.Thinking)
        AiSummaryCard(state = AiSummaryState.Streaming(""))
        AiSummaryCard(
            state =
                AiSummaryState.Done(
                    "文章介绍了 ReadYou 的 Material You 取色方式，并说明动态配色来自壁纸。" +
                        "作者认为阅读器应以内容为中心，减少干扰元素。",
                ),
        )
        AiSummaryCard(state = AiSummaryState.Error("HTTP 401: incorrect API key"))
    }
}

/** Title states: Thinking shows "正在思考…", Streaming shows "AI Summary". */
@Preview(widthDp = 340, heightDp = 220, showBackground = true, backgroundColor = 0xFFFFF8F6)
@Composable
fun AiSummaryThinkingComparePrev() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("THINKING (reasoning in progress)", style = MaterialTheme.typography.labelSmall)
        AiSummaryCard(state = AiSummaryState.Thinking)
        Text("STREAMING (answer arriving)", style = MaterialTheme.typography.labelSmall)
        AiSummaryCard(state = AiSummaryState.Streaming("文章介绍了 ReadYou 的 Material You 取色方式。"))
    }
}
