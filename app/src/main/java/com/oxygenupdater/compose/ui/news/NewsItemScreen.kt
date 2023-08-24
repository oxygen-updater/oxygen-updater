package com.oxygenupdater.compose.ui.news

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.text.format.DateUtils
import android.webkit.WebResourceError
import android.webkit.WebSettings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberSaveableWebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.android.gms.ads.AdView
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.BannerAd
import com.oxygenupdater.compose.ui.common.GridItem
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.light
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.models.NewsItem
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsItemScreen(
    state: RefreshAwareState<NewsItem?>,
    scrollBehavior: TopAppBarScrollBehavior,
    webViewState: WebViewState,
    navigator: WebViewNavigator,
    showAds: Boolean,
    bannerAdInit: (AdView) -> Unit,
    onError: (String) -> Unit,
    onLoadFinished: (NewsItem) -> Unit,
) {
    val (refreshing, data) = state
    val item = rememberSaveable(data ?: return) { data }

    Column(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
        val adLoaded = remember { mutableStateOf(false) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()) // must be after `nestedScroll`
        ) {
            Buttons(item)

            var showDivider = false
            val modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            val textModifier = Modifier.withPlaceholder(refreshing)
            val caption = MaterialTheme.typography.bodySmall

            item.subtitle?.let {
                showDivider = true
                IconText(
                    modifier, textModifier,
                    icon = Icons.Rounded.Notes, text = it,
                    style = caption
                )
            }

            item.getRelativeTime()?.let {
                showDivider = true
                IconText(
                    modifier, textModifier,
                    icon = Icons.Rounded.Schedule, text = it,
                    style = caption
                )
            }

            // We can't edit CSS in WebViews, so pass theme to backend to style it accordingly
            val url = item.apiUrl + if (MaterialTheme.colorScheme.light) "Light" else "Dark"
            RefreshAwareWebView(refreshing, webViewState, navigator, url, showDivider, onError, adLoaded.value) {
                onLoadFinished(item)
            }
        }

        if (showAds) {
            ItemDivider()
            BannerAd(BuildConfig.AD_BANNER_NEWS_ID, adLoaded, bannerAdInit)
        }
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun Buttons(item: NewsItem) = with(LocalContext.current) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        // 32dp total vertical padding + 24dp icon
        Modifier.height(56.dp),
        userScrollEnabled = false
    ) {
        item {
            GridItem(Icons.Outlined.Share, androidx.browser.R.string.fallback_menu_item_share_link) { shareExternally(item.title ?: "", item.webUrl) }
        }

        item {
            GridItem(Icons.Rounded.Link, androidx.browser.R.string.fallback_menu_item_copy_link) { copyToClipboard(item.webUrl) }
        }
    }
}

@Composable
fun ColumnScope.ErrorSheet(
    title: String,
    dismiss: () -> Unit,
    confirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        title,
        Modifier.padding(start = 16.dp, end = 8.dp),
        color = colorScheme.primary,
        overflow = TextOverflow.Ellipsis, maxLines = 1,
        style = MaterialTheme.typography.titleMedium
    )

    Text(
        stringResource(R.string.news_load_network_error),
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState()),
        style = MaterialTheme.typography.bodyMedium
    )

    Row(
        Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            dismiss,
            Modifier.padding(end = 8.dp),
            colors = textButtonColors(contentColor = colorScheme.error)
        ) {
            Text(stringResource(R.string.download_error_close))
        }

        OutlinedIconButton({
            confirm()
            dismiss()
        }, Icons.Rounded.Autorenew, R.string.download_error_retry)
    }
}

@Composable
private fun RefreshAwareWebView(
    refreshing: Boolean,
    webViewState: WebViewState,
    navigator: WebViewNavigator,
    url: String,
    showDivider: Boolean,
    onError: (String) -> Unit,
    adLoaded: Boolean,
    onLoadFinished: () -> Unit,
) {
    LaunchedEffect(navigator) {
        // null viewState => first load
        if (webViewState.viewState == null) navigator.loadUrl(url)
    }

    val loading = webViewState.loadingState
    val paddingTop = if (showDivider) Modifier.padding(top = 13.dp) else Modifier
    if (refreshing || loading == LoadingState.Initializing) LinearProgressIndicator(
        paddingTop.fillMaxWidth()
    ) else if (loading is LoadingState.Loading) LinearProgressIndicator(
        loading.progress, paddingTop.fillMaxWidth()
    ) else if (loading == LoadingState.Finished) {
        val error = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Errors not for our URLs are filtered out in [WebViewClient]
            webViewState.errorsForCurrentRequest.firstOrNull()?.error?.errorString()
        } else null

        // Ensure finished & error callbacks are done just once per load
        LaunchedEffect(error) {
            if (error == null) onLoadFinished() else onError(error)
        }

        if (showDivider) ItemDivider(Modifier.padding(top = 16.dp))
    }

    val context = LocalContext.current
    val client = remember(context) { WebViewClient(context) }
    val userAgent = OxygenUpdater.APP_USER_AGENT + try {
        " " + WebSettings.getDefaultUserAgent(context)
    } catch (e: Exception) {
        ""
    }

    // TODO(compose/news): add scrollbars when it's out as first-party solution: https://developer.android.com/jetpack/androidx/compose-roadmap
    val runningInPreview = LocalInspectionMode.current
    WebView(
        webViewState,
        // Don't re-consume navigation bar insets
        (if (adLoaded) Modifier else Modifier.navigationBarsPadding())
            // news-content HTML already has an 8px margin
            .padding(horizontal = 8.dp),
        navigator = navigator,
        onCreated = {
            // Must be done to avoid the white background in dark themes
            it.setBackgroundColor(Color.TRANSPARENT)

            // AndroidViews are not supported by preview, bail early
            if (runningInPreview) return@WebView

            @SuppressLint("SetJavaScriptEnabled")
            it.settings.javaScriptEnabled = true
            it.settings.userAgentString = userAgent
        }, client = client
    )
}

@RequiresApi(Build.VERSION_CODES.M)
private fun WebResourceError.errorString() = description?.toString() ?: when (errorCode) {
    // Basic errors
    android.webkit.WebViewClient.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
    android.webkit.WebViewClient.ERROR_HOST_LOOKUP -> "ERROR_HOST_LOOKUP"
    android.webkit.WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "ERROR_UNSUPPORTED_AUTH_SCHEME"
    android.webkit.WebViewClient.ERROR_AUTHENTICATION -> "ERROR_AUTHENTICATION"
    android.webkit.WebViewClient.ERROR_PROXY_AUTHENTICATION -> "ERROR_PROXY_AUTHENTICATION"
    android.webkit.WebViewClient.ERROR_CONNECT -> "ERROR_CONNECT"
    android.webkit.WebViewClient.ERROR_IO -> "ERROR_IO"
    android.webkit.WebViewClient.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
    android.webkit.WebViewClient.ERROR_REDIRECT_LOOP -> "ERROR_REDIRECT_LOOP"
    android.webkit.WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERROR_UNSUPPORTED_SCHEME"
    android.webkit.WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ERROR_FAILED_SSL_HANDSHAKE"
    android.webkit.WebViewClient.ERROR_BAD_URL -> "ERROR_BAD_URL"
    android.webkit.WebViewClient.ERROR_FILE -> "ERROR_FILE"
    android.webkit.WebViewClient.ERROR_FILE_NOT_FOUND -> "ERROR_FILE_NOT_FOUND"
    android.webkit.WebViewClient.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
    android.webkit.WebViewClient.ERROR_UNSAFE_RESOURCE -> "ERROR_UNSAFE_RESOURCE"
    // SSL errors, in order of severity
    SslError.SSL_NOTYETVALID -> "SSL_NOT_YET_VALID"
    SslError.SSL_EXPIRED -> "SSL_EXPIRED"
    SslError.SSL_IDMISMATCH -> "SSL_ID_MISMATCH"
    SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
    SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
    SslError.SSL_INVALID -> "SSL_INVALID"
    // Assume it's an HTTP error
    else -> "ERROR_$errorCode"
}

private fun NewsItem.getRelativeTime() = epochMilli?.let {
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

@OptIn(ExperimentalMaterial3Api::class)
@PreviewThemes
@Composable
fun PreviewNewsItemScreen() = PreviewAppTheme {
    val date = LocalDateTime.now().toString()
    NewsItemScreen(
        RefreshAwareState(
            false, NewsItem(
                1,
                title = "An unnecessarily long news title, to get an accurate understanding of how long titles are rendered",
                subtitle = "An unnecessarily long news subtitle, to get an accurate understanding of how long subtitles are rendered",
                imageUrl = "https://github.com/oxygen-updater.png",
                text = "Text",
                datePublished = date,
                dateLastEdited = date,
                authorName = "Author",
                read = false,
            )
        ),
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        webViewState = rememberSaveableWebViewState(),
        navigator = rememberWebViewNavigator(),
        showAds = true,
        bannerAdInit = {},
        onError = {},
        onLoadFinished = {},
    )
}
