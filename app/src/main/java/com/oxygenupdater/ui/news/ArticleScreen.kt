package com.oxygenupdater.ui.news

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.models.Article
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.GridItem
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.LazyVerticalGrid
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.ArticleErrorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.light
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    viewModel: ArticleViewModel,
    id: Long,
    scrollBehavior: TopAppBarScrollBehavior,
    loadInterstitialAd: () -> Unit,
) {
    DisposableEffect(Unit) {
        viewModel.refreshItem(id)
        // Clear to avoid loading the previous article in the WebView
        onDispose(viewModel::clearItem)
    }

    val webViewState = rememberWebViewState()
    val onRefresh: () -> Unit = {
        loadInterstitialAd()

        viewModel.refreshItem(id)
        webViewState.webView?.reload()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    ArticleScreen(
        state = state,
        onRefresh = onRefresh,
        webViewState = webViewState,
        onLoadFinished = viewModel::markRead,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    )
}

@VisibleForTesting
@Composable
fun ArticleScreen(
    modifier: Modifier,
    state: RefreshAwareState<Article?>,
    onRefresh: () -> Unit,
    webViewState: WebViewState,
    onLoadFinished: (Article) -> Unit,
) {
    val (refreshing, data) = state
    // TODO(compose): remove `key` if https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269 is resolved
    val item = rememberSaveable(data ?: return, key = data.id.toString()) { data }

    PullRefresh(
        state = state,
        shouldShowProgressIndicator = { it?.isFullyLoaded != true },
        onRefresh = onRefresh,
    ) {
        var errorTitle by remember { mutableStateOf<String?>(null) }
        errorTitle?.let { title ->
            ModalBottomSheet({ errorTitle = null }) { ArticleErrorSheet(it, title, confirm = onRefresh) }
        }

        Column(modifier) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()) // must be after `nestedScroll`
            ) {
                Buttons(item = item)

                var showDivider = false
                val bodySmall = MaterialTheme.typography.bodySmall
                val textModifier = Modifier.withPlaceholder(refreshing, bodySmall)

                item.subtitle?.let {
                    showDivider = true
                    IconText(
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        text = it,
                        style = bodySmall,
                        textModifier = textModifier,
                        modifier = modifierDefaultPaddingStartTopEnd
                    )
                }

                item.getRelativeTime()?.let {
                    showDivider = true
                    IconText(
                        icon = Icons.Rounded.Schedule,
                        text = it,
                        style = bodySmall,
                        textModifier = textModifier,
                        modifier = modifierDefaultPaddingStartTopEnd
                    )
                }

                // We can't edit CSS in WebViews, so pass theme to backend to style it accordingly
                val language = if (currentLocale().language == "nl") "NL" else "EN"
                val theme = if (MaterialTheme.colorScheme.light) "Light" else "Dark"
                val url = "${item.apiUrl}/$language/$theme"
                RefreshAwareWebView(
                    refreshing = refreshing,
                    webViewState = webViewState,
                    url = url,
                    showDivider = showDivider,
                    onError = { errorTitle = it },
                    onLoadFinished = { onLoadFinished(item) },
                )
            }
        }
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun Buttons(item: Article) = with(LocalContext.current) {
    LazyVerticalGrid(
        columnCount = 2,
        items = arrayOf(
            GridItem(Icons.Outlined.Share, androidx.browser.R.string.fallback_menu_item_share_link) {
                shareExternally(item.title ?: "", item.webUrl)
            },
            GridItem(Icons.Rounded.Link, androidx.browser.R.string.fallback_menu_item_copy_link) {
                copyToClipboard(item.webUrl)
            },
        ),
    )
}

@Composable
private fun RefreshAwareWebView(
    refreshing: Boolean,
    webViewState: WebViewState,
    url: String,
    showDivider: Boolean,
    onError: (String) -> Unit,
    onLoadFinished: () -> Unit,
) {
    LaunchedEffect(url) { webViewState.webView?.loadUrl(url) }

    val loading = webViewState.loadingState
    val paddingTop = if (showDivider) Modifier.padding(top = 13.dp) else Modifier
    if (refreshing || loading == LoadingState.Initializing) LinearProgressIndicator(
        paddingTop then modifierMaxWidth
    ) else if (loading is LoadingState.Loading) LinearProgressIndicator(
        progress = { loading.progress },
        modifier = paddingTop then modifierMaxWidth
    ) else if (loading == LoadingState.Finished) {
        val error = if (SDK_INT >= VERSION_CODES.M) {
            /** Errors not for our URLs are filtered out in [WebViewClient] */
            webViewState.errorForCurrentRequest?.errorCodeString
        } else null

        // Ensure finished & error callbacks are done just once per load
        LaunchedEffect(error) {
            if (error == null) onLoadFinished() else onError(error)
        }

        if (showDivider) HorizontalDivider(modifierDefaultPaddingTop)
    }

    // TODO(compose/news): add scrollbars when it's out as first-party solution: https://developer.android.com/jetpack/androidx/compose-roadmap.
    // Also see https://github.com/android/nowinandroid/pull/722.
    WebView(
        state = webViewState,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 8.dp) // news-content HTML already has an 8px margin
    )
}

@VisibleForTesting
fun Article.getRelativeTime() = epochMilli?.let {
    // Weird bug in `android.text.format` that causes a crash in
    // older Android versions, due to using the [FORMAT_SHOW_TIME]
    // flag. Hacky workaround is to call the function again, but
    // without this flag. See https://stackoverflow.com/a/52665211.
    try {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    } catch (e: NullPointerException) {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }
}?.toString()

@VisibleForTesting
val PreviewArticleData = LocalDateTime.now().let { now ->
    Article(
        id = 1,
        title = "An unnecessarily long news title, to get an accurate understanding of how long titles are rendered",
        subtitle = "An unnecessarily long news subtitle, to get an accurate understanding of how long subtitles are rendered",
        imageUrl = "https://github.com/oxygen-updater.png",
        text = "Text",
        datePublished = now.minusHours(2).toString(),
        dateLastEdited = now.toString(),
        authorName = "Author",
        read = false,
    )
}

@PreviewThemes
@Composable
fun PreviewArticleScreen() = PreviewAppTheme {
    ArticleScreen(
        state = RefreshAwareState(false, PreviewArticleData),
        onRefresh = {},
        webViewState = rememberWebViewState(),
        onLoadFinished = {},
        modifier = Modifier,
    )
}
