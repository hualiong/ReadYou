package me.ash.reader.ui.page.home.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import java.util.Date
import me.ash.reader.infrastructure.preference.LocalReadingRenderer
import me.ash.reader.infrastructure.preference.LocalReadingSubheadUpperCase
import me.ash.reader.infrastructure.preference.ReadingRendererPreference
import me.ash.reader.ui.component.reader.LocalTextContentWidth
import me.ash.reader.ui.component.reader.Reader
import me.ash.reader.ui.component.reader.textHorizontalPadding
import me.ash.reader.ui.component.scrollbar.drawVerticalScrollIndicator
import me.ash.reader.ui.component.webview.RYWebView
import me.ash.reader.ui.page.adaptive.AiSummaryState
import me.ash.reader.ui.ext.extractDomain
import me.ash.reader.ui.ext.roundClick

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Content(
    modifier: Modifier = Modifier,
    content: String,
    feedName: String,
    title: String,
    author: String? = null,
    link: String? = null,
    publishedDate: Date,
    scrollState: ScrollState,
    listState: LazyListState,
    isLoading: Boolean,
    aiSummaryState: AiSummaryState = AiSummaryState.Idle,
    aiSummaryVisible: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    onImageClick: ((imgUrl: String, altText: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val subheadUpperCase = LocalReadingSubheadUpperCase.current
    val renderer = LocalReadingRenderer.current

    val textContentWidth = LocalTextContentWidth.current
    val maxWidthModifier = Modifier.widthIn(max = textContentWidth)
    val uriHandler = LocalUriHandler.current

    val headline =
        @Composable {
            Column(modifier = Modifier.then(maxWidthModifier).padding(horizontal = 12.dp)) {
                DisableSelection {
                    Metadata(
                        feedName = feedName,
                        title = title,
                        author = author,
                        publishedDate = publishedDate,
                        modifier = Modifier.roundClick { link?.let { uriHandler.openUri(it) } },
                    )
                }
            }
        }

    // The exit animation keeps composing the card, so hold the last non-Idle state:
    // stopping mid-stream sets Idle, which would otherwise empty the card before it shrinks.
    val shownSummary = remember { mutableStateOf<AiSummaryState>(AiSummaryState.Idle) }
    if (aiSummaryState != AiSummaryState.Idle) shownSummary.value = aiSummaryState

    val summaryCard =
        @Composable {
            AnimatedVisibility(
                visible = aiSummaryVisible,
                enter = expandVertically(tween(300)) + fadeIn(tween(200, delayMillis = 100)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(250, delayMillis = 50)),
            ) {
                val horizontal = textHorizontalPadding().dp
                Column(
                    modifier =
                        Modifier.then(maxWidthModifier)
                            .padding(start = horizontal, end = horizontal, top = 12.dp, bottom = 6.dp)
                ) {
                    AiSummaryCard(state = shownSummary.value)
                }
            }
        }

    if (isLoading) {
        Column { LoadingIndicator(modifier = Modifier.size(56.dp)) }
    } else {

        when (renderer) {
            ReadingRendererPreference.WebView -> {
                Column(
                    modifier =
                        modifier
                            .padding(top = contentPadding.calculateTopPadding())
                            .fillMaxSize()
                            .drawVerticalScrollIndicator(scrollState)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(modifier = Modifier.then(maxWidthModifier)) {
                            // Top bar height
                            Spacer(modifier = Modifier.height(64.dp))
                            // padding
                            headline()

                            summaryCard()

                            RYWebView(
                                modifier = Modifier.fillMaxSize(),
                                content = content,
                                refererDomain = link.extractDomain(),
                                onImageClick = onImageClick,
                            )
                            Spacer(modifier = Modifier.height(128.dp))
                            Spacer(
                                modifier = Modifier.height(contentPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }

            ReadingRendererPreference.NativeComponent -> {
                SelectionContainer {
                    LazyColumn(
                        modifier = modifier.fillMaxSize().drawVerticalScrollIndicator(listState),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            // Top bar height
                            Spacer(modifier = Modifier.height(64.dp))
                            // padding
                            Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
                            headline()
                        }

                        item(key = "ai_summary") {
                            summaryCard()
                        }

                        Reader(
                            context = context,
                            subheadUpperCase = subheadUpperCase.value,
                            link = link ?: "",
                            content = content,
                            onImageClick = onImageClick,
                            onLinkClick = { uriHandler.openUri(it) },
                        )

                        item {
                            Spacer(modifier = Modifier.height(128.dp))
                            Spacer(
                                modifier = Modifier.height(contentPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }
        }
    }
}
