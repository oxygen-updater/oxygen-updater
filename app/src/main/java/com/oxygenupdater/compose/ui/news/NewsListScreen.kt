package com.oxygenupdater.compose.ui.news

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.compose.activities.NewsItemActivity
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Image
import com.oxygenupdater.compose.icons.Info
import com.oxygenupdater.compose.icons.NewsMultiple
import com.oxygenupdater.compose.ui.PullRefresh
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.DropdownMenuItem
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.openInCustomTab
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.utils.Utils
import java.time.LocalDateTime
import kotlin.random.Random

var previousUnreadCount = 0

@Composable
fun NewsListScreen(
    state: RefreshAwareState<List<NewsItem>>,
    refresh: () -> Unit,
    markAllRead: () -> Unit,
    toggleRead: (NewsItem) -> Unit,
    openItem: (Long) -> Unit,
) = PullRefresh(state, shouldShowProgressIndicator = {
    it.isEmpty()
}, onRefresh = refresh) {
    val refreshing = state.refreshing
    var onlyUnread by remember { mutableStateOf(false) }

    val data = if (onlyUnread) state.data.filterNot { it.read } else state.data
    val list = if (!refreshing) rememberSaveable(onlyUnread) { data } else data
    var unreadCount by remember(onlyUnread) {
        mutableIntStateOf(if (onlyUnread) list.size else list.count { !it.read })
    }

    if (unreadCount != previousUnreadCount) {
        // Display badge with the number of unread news articles
        // If there aren't any unread articles, the badge is hidden
        Screen.NewsList.badge = if (unreadCount == 0) null else "$unreadCount"
        previousUnreadCount = unreadCount
    }

    Column {
        var showBannerMenu by remember { mutableStateOf(false) }
        Banner(showBannerMenu, stringResource(
            if (onlyUnread) R.string.news_unread_count_2
            else R.string.news_unread_count_1,
            unreadCount
        ), onDismiss = {
            showBannerMenu = false
        }, onLongClick = {
            showBannerMenu = true
        }, onClick = {
            onlyUnread = !onlyUnread
        }) {
            markAllRead()
            list.forEach { it.read = true }
            unreadCount = 0
        }

        ItemDivider()

        if (onlyUnread && unreadCount == 0) EmptyState(true)
        else if (list.isEmpty()) EmptyState(false)
        else LazyColumn(Modifier.fillMaxHeight()) {
            items(list, { it.id ?: Random.nextLong() }) {
                NewsListItem(refreshing, it, toggleRead = {
                    toggleRead(it)
                    unreadCount += if (it.read) 1 else -1
                }) {
                    NewsItemActivity.item = it
                    openItem(it.id ?: -1L)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(allRead: Boolean) = Column(
    Modifier.fillMaxHeight(), Arrangement.Center, Alignment.CenterHorizontally
) {
    Text(
        stringResource(
            if (allRead) R.string.news_empty_state_all_read_header
            else R.string.news_empty_state_none_available_header
        ),
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h6
    )
    Icon(
        CustomIcons.NewsMultiple, null,
        Modifier.requiredSize(150.dp),
        tint = MaterialTheme.colors.primary
    )
    Text(
        stringResource(
            if (allRead) R.string.news_empty_state_all_read_text
            else R.string.news_empty_state_none_available_text
        ),
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body2
    )
}

@Composable
private fun NewsListItem(
    refreshing: Boolean,
    item: NewsItem,
    toggleRead: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    var showItemMenu by remember { mutableStateOf(false) }
    @OptIn(ExperimentalFoundationApi::class)
    Column(Modifier.combinedClickable(!refreshing, onLongClick = { showItemMenu = true }) {
        onClick()
    }) {
        Box {
            if (!refreshing && !item.read) Badge(Modifier.offset(4.dp, 16.dp), MaterialTheme.colors.primary)

            ItemMenu(showItemMenu, { showItemMenu = false }, item, toggleRead)

            Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .requiredHeight(80.dp)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        item.title ?: "Unknown title",
                        Modifier.withPlaceholder(refreshing),
                        overflow = TextOverflow.Ellipsis, maxLines = 2,
                        style = MaterialTheme.typography.subtitle1
                    )
                    AutoresizeText(
                        item.subtitle ?: "",
                        FontSizeRange(12.sp, 14.sp),
                        Modifier
                            .alpha(ContentAlpha.medium)
                            .withPlaceholder(refreshing)
                    )
                }

                val defaultImage = CustomIcons.Image.run {
                    rememberVectorPainter(
                        defaultWidth = defaultWidth,
                        defaultHeight = defaultHeight,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        name = name,
                        tintColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        tintBlendMode = tintBlendMode,
                        autoMirror = autoMirror,
                        content = { _, _ -> RenderVectorGroup(group = root) }
                    )
                }
                AsyncImage(
                    item.imageUrl?.let {
                        remember(it) {
                            ImageRequest.Builder(context)
                                .data(it)
                                .size(Utils.dpToPx(context, 80f).toInt())
                                .build()
                        }
                    },
                    stringResource(R.string.icon),
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .requiredSize(80.dp)
                        .withPlaceholder(refreshing),
                    placeholder = defaultImage,
                    error = defaultImage,
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val authorName = item.authorName ?: "Unknown Author"

            Text(
                item.epochMilli?.let {
                    DateUtils.getRelativeTimeSpanString(
                        it,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL
                    )
                }?.let {
                    "$it \u2022 $authorName"
                } ?: authorName,
                (if (refreshing) Modifier else Modifier.basicMarquee())
                    .weight(1f)
                    .padding(start = 16.dp)
                    .alpha(ContentAlpha.medium)
                    .withPlaceholder(refreshing),
                maxLines = 1,
                style = MaterialTheme.typography.caption
            )

            IconButton({ showItemMenu = true }, Modifier.offset(x = 8.dp), !refreshing) {
                CompositionLocalProvider(
                    LocalContentAlpha provides if (refreshing) {
                        LocalContentAlpha.current
                    } else ContentAlpha.medium
                ) {
                    Icon(
                        Icons.Rounded.MoreVert, stringResource(R.string.icon),
                        Modifier.requiredSize(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Banner(
    expanded: Boolean,
    text: String,
    onDismiss: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
) {
    @OptIn(ExperimentalFoundationApi::class)
    IconText(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(16.dp), // must be after `clickable`
        icon = CustomIcons.Info, text = text,
    ) { BannerMenu(expanded, onDismiss, onMarkAllReadClick) }
}

@Composable
private fun BannerMenu(
    showBannerMenu: Boolean,
    onDismiss: () -> Unit,
    onMarkAllReadClick: () -> Unit,
) = DropdownMenu(showBannerMenu, onDismiss, offset = DpOffset(24.dp, 0.dp)) {
    DropdownMenuItem(Icons.Rounded.PlaylistAddCheck, R.string.news_mark_all_read) {
        onMarkAllReadClick()
        onDismiss()
    }
}

@Composable
private fun ItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: NewsItem,
    onToggleReadClick: () -> Unit,
) = DropdownMenu(expanded, onDismiss, offset = DpOffset(1000.dp, 0.dp)) {
    val context = LocalContext.current

    DropdownMenuItem(
        if (item.read) Icons.Rounded.HighlightOff else Icons.Rounded.CheckCircleOutline,
        if (item.read) R.string.news_mark_unread else R.string.news_mark_read,
    ) {
        onToggleReadClick()
        item.read = !item.read

        onDismiss()
    }

    DropdownMenuItem(Icons.Rounded.OpenInBrowser, androidx.browser.R.string.fallback_menu_item_open_in_browser) {
        context.openInCustomTab(item.webUrl)
        onDismiss()
    }

    DropdownMenuItem(Icons.Outlined.Share, androidx.browser.R.string.fallback_menu_item_share_link) {
        context.shareExternally(item.title ?: "", item.webUrl)
        onDismiss()
    }

    DropdownMenuItem(Icons.Rounded.Link, androidx.browser.R.string.fallback_menu_item_copy_link) {
        context.copyToClipboard(item.webUrl)
        onDismiss()
    }
}

/**
 * @see <a href="https://stackoverflow.com/a/69780826">stackoverflow.com/a/69780826<a>
 */
@Composable
private fun AutoresizeText(
    text: String,
    fontSizeRange: FontSizeRange,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    val (min, max, step) = fontSizeRange
    var fontSizeValue by remember { mutableFloatStateOf(max.value) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text,
        modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        fontSize = fontSizeValue.sp,
        overflow = TextOverflow.Ellipsis, maxLines = maxLines,
        onTextLayout = {
            // Fits before reaching min, mark readyToDraw
            if (!it.didOverflowHeight) readyToDraw = true
            else if (!readyToDraw) {
                val nextFontSizeValue = fontSizeValue - step.value
                if (nextFontSizeValue <= min.value) {
                    // Reached min, set & mark readyToDraw
                    fontSizeValue = min.value
                    readyToDraw = true
                } else {
                    // Doesn't fit yet and we haven't reached min, keep decreasing
                    fontSizeValue = nextFontSizeValue
                }
            }
        },
        style = MaterialTheme.typography.body2
    )
}

private data class FontSizeRange(
    val min: TextUnit,
    val max: TextUnit,
    val step: TextUnit = DEFAULT_STEP,
) {
    init {
        require(min <= max) { "min should be less than or equal to max, $this" }
        require(step.value > 0) { "step should be greater than 0, $this" }
    }

    companion object {
        private val DEFAULT_STEP = 1.sp
    }
}

@PreviewThemes
@Composable
fun PreviewNewsListScreen() = PreviewAppTheme {
    val now = LocalDateTime.now()
    val longEn = "Unnecessarily long text, to get an accurate understanding of how its rendered"
    val longNl = "Onnodig lange tekst, om goed te begrijpen hoe het wordt weergegeven"
    NewsListScreen(
        RefreshAwareState(
            false, listOf(
                NewsItem(
                    1,
                    dutchTitle = stringResource(R.string.app_name),
                    englishTitle = stringResource(R.string.app_name),
                    dutchSubtitle = longNl,
                    englishSubtitle = longEn,
                    imageUrl = "https://github.com/oxygen-updater.png",
                    dutchText = longNl,
                    englishText = longEn,
                    datePublished = now.minusDays(1).toString(),
                    dateLastEdited = now.minusHours(4).toString(),
                    authorName = "Author",
                    read = false,
                ),
                NewsItem(
                    2,
                    dutchTitle = longNl,
                    englishTitle = longEn,
                    dutchSubtitle = longNl,
                    englishSubtitle = longEn,
                    imageUrl = "https://github.com/oxygen-updater.png",
                    dutchText = longNl,
                    englishText = longEn,
                    datePublished = now.minusDays(2).toString(),
                    dateLastEdited = now.minusHours(5).toString(),
                    authorName = "Author",
                    read = false,
                ),
            )
        ),
        refresh = {},
        markAllRead = {},
        toggleRead = {},
    ) {}
}

@PreviewThemes
@Composable
fun PreviewEmptyState() = PreviewAppTheme {
    EmptyState(false)
}

