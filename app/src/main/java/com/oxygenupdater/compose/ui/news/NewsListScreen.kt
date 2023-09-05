package com.oxygenupdater.compose.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
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
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.common.DropdownMenuItem
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.PullRefresh
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberSaveableState
import com.oxygenupdater.compose.ui.common.withPlaceholder
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.models.NewsItem
import java.time.LocalDateTime
import kotlin.random.Random

@Composable
fun NewsListScreen(
    state: RefreshAwareState<List<NewsItem>>,
    refresh: () -> Unit,
    unreadCountState: MutableIntState,
    markAllRead: () -> Unit,
    toggleRead: (NewsItem) -> Unit,
    openItem: (Long) -> Unit,
) = PullRefresh(state, { it.isEmpty() }, refresh) {
    val refreshing = state.refreshing
    var onlyUnread by rememberSaveableState("onlyUnread", false)

    val data = if (onlyUnread) state.data.filterNot { it.readState.value } else state.data
    val list = if (!refreshing) rememberSaveable(onlyUnread) { data } else data

    LaunchedEffect(onlyUnread) {
        unreadCountState.intValue = if (onlyUnread) list.size else list.count { !it.readState.value }
    }

    val unreadCount = unreadCountState.intValue
    Screen.NewsList.badge = unreadCount.let { if (it == 0) null else it.toString() }

    Column {
        Banner(stringResource(
            if (onlyUnread) R.string.news_unread_count_2 else R.string.news_unread_count_1,
            unreadCount
        ), onClick = {
            onlyUnread = !onlyUnread
        }, onMarkAllReadClick = {
            markAllRead()
            unreadCountState.intValue = 0
        })

        ItemDivider()

        if (onlyUnread && unreadCount == 0) EmptyState(true)
        else if (list.isEmpty()) EmptyState(false)
        else LazyColumn(Modifier.fillMaxHeight()) {
            items(list, { it.id ?: Random.nextLong() }) {
                NewsListItem(refreshing, it, toggleRead = {
                    toggleRead(it)
                    unreadCountState.intValue += if (it.readState.value) 1 else -1
                }) {
                    NewsItemActivity.item = it
                    openItem(it.id ?: NOT_SET_L)
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
        style = MaterialTheme.typography.titleLarge
    )
    Icon(
        CustomIcons.NewsMultiple, null,
        Modifier.requiredSize(150.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Text(
        stringResource(
            if (allRead) R.string.news_empty_state_all_read_text
            else R.string.news_empty_state_none_available_text
        ),
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NewsListItem(
    refreshing: Boolean,
    item: NewsItem,
    toggleRead: () -> Unit,
    onClick: () -> Unit,
) = Column(Modifier.clickable(!refreshing, onClick = onClick)) {
    Box {
        val read by remember { item.readState }
        if (!refreshing && !read) Badge(Modifier.offset(4.dp, 16.dp))

        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .requiredHeight(80.dp) // same as image
                    .padding(end = 8.dp)
            ) {
                Text(
                    item.title ?: "Unknown title",
                    Modifier
                        .withPlaceholder(refreshing)
                        .graphicsLayer {
                            if (refreshing) return@graphicsLayer
                            alpha = if (read) 0.7f else 1f
                        },
                    overflow = TextOverflow.Ellipsis, maxLines = 2,
                    style = MaterialTheme.typography.titleMedium
                )
                AutoresizeText(
                    item.subtitle ?: "",
                    FontSizeRange(12f, 14f),
                    Modifier.withPlaceholder(refreshing)
                )
            }

            val defaultImage = CustomIcons.Image.run {
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    name = name,
                    tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tintBlendMode = tintBlendMode,
                    autoMirror = autoMirror,
                    content = { _, _ -> RenderVectorGroup(group = root) }
                )
            }
            val context = LocalContext.current
            AsyncImage(
                item.imageUrl?.let {
                    val size = LocalDensity.current.run { 80.dp.roundToPx() }
                    remember(it, size) {
                        ImageRequest.Builder(context)
                            .data(it)
                            .size(size)
                            .build()
                    }
                },
                stringResource(R.string.icon),
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .requiredSize(80.dp)
                    .withPlaceholder(refreshing)
                    .graphicsLayer {
                        if (refreshing) return@graphicsLayer
                        alpha = if (read) 0.87f else 1f
                    },
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
            Modifier
                .padding(start = 16.dp)
                .weight(1f)
                .basicMarquee()
                .withPlaceholder(refreshing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )

        ItemMenuOpener(refreshing, item, toggleRead)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Banner(
    text: String,
    onClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
) {
    var showMenu by rememberSaveableState("showBannerMenu", false)
    IconText(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = { showMenu = true }, onClick = onClick)
            .padding(16.dp), // must be after `clickable`
        icon = CustomIcons.Info, text = text,
    ) {
        DropdownMenu(showMenu, { showMenu = false }, offset = DpOffset(24.dp, 0.dp)) {
            DropdownMenuItem(Icons.AutoMirrored.Rounded.PlaylistAddCheck, R.string.news_mark_all_read) {
                onMarkAllReadClick()
                showMenu = false
            }
        }
    }
}

@Composable
private fun ItemMenuOpener(
    refreshing: Boolean,
    item: NewsItem,
    toggleRead: () -> Unit,
) = Box {
    var showMenu by rememberSaveableState("showItemMenu", false)
    ItemMenu(showMenu, { showMenu = false }, item, toggleRead)

    // Offset by 6.dp to bring in line with image
    IconButton({ showMenu = true }, Modifier.offset(x = 6.dp), !refreshing) {
        Icon(
            Icons.Rounded.MoreVert, stringResource(R.string.icon),
            Modifier.requiredSize(20.dp),
            if (refreshing) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun ItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: NewsItem,
    onToggleReadClick: () -> Unit,
) = DropdownMenu(expanded, onDismiss, offset = DpOffset(0.dp, (-40).dp)) {
    var read by remember { item.readState }
    DropdownMenuItem(
        if (read) Icons.Rounded.HighlightOff else Icons.Rounded.CheckCircleOutline,
        if (read) R.string.news_mark_unread else R.string.news_mark_read,
    ) {
        onToggleReadClick()
        read = !read

        onDismiss()
    }

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    DropdownMenuItem(Icons.Rounded.OpenInBrowser, androidx.browser.R.string.fallback_menu_item_open_in_browser, rememberCallback(context, customTabIntent) {
        customTabIntent.launch(context, item.webUrl)
        onDismiss()
    })

    DropdownMenuItem(Icons.Outlined.Share, androidx.browser.R.string.fallback_menu_item_share_link, rememberCallback(context) {
        context.shareExternally(item.title ?: "", item.webUrl)
        onDismiss()
    })

    DropdownMenuItem(Icons.Rounded.Link, androidx.browser.R.string.fallback_menu_item_copy_link, rememberCallback(context) {
        context.copyToClipboard(item.webUrl)
        onDismiss()
    })
}

// TODO(compose/news): switch to first-party solution when it's out: https://developer.android.com/jetpack/androidx/compose-roadmap#core-libraries
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
    var fontSizeValue by remember { mutableFloatStateOf(max) }
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
            if (!it.hasVisualOverflow) readyToDraw = true
            else if (!readyToDraw) {
                val nextFontSizeValue = fontSizeValue - step
                if (nextFontSizeValue <= min) {
                    // Reached min, set & mark readyToDraw
                    fontSizeValue = min
                    readyToDraw = true
                } else {
                    // Doesn't fit yet and we haven't reached min, keep decreasing
                    fontSizeValue = nextFontSizeValue
                }
            }
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Immutable
private data class FontSizeRange(
    val min: Float,
    val max: Float,
    val step: Float = DEFAULT_STEP,
) {
    init {
        require(min <= max) { "min should be less than or equal to max, $this" }
        require(step > 0) { "step should be greater than 0, $this" }
    }

    companion object {
        private const val DEFAULT_STEP = 1f
    }
}

@PreviewThemes
@Composable
fun PreviewNewsListScreen() = PreviewAppTheme {
    val now = LocalDateTime.now()
    val long = "Unnecessarily long text, to get an accurate understanding of how its rendered"
    NewsListScreen(
        RefreshAwareState(
            false, listOf(
                NewsItem(
                    1,
                    title = stringResource(R.string.app_name),
                    subtitle = long,
                    imageUrl = "https://github.com/oxygen-updater.png",
                    text = long,
                    datePublished = now.minusDays(1).toString(),
                    dateLastEdited = now.minusHours(4).toString(),
                    authorName = "Author",
                    read = true,
                ),
                NewsItem(
                    2,
                    title = long,
                    subtitle = long,
                    imageUrl = "https://github.com/oxygen-updater.png",
                    text = long,
                    datePublished = now.minusDays(2).toString(),
                    dateLastEdited = now.minusHours(5).toString(),
                    authorName = "Author",
                    read = false,
                ),
            )
        ),
        refresh = {},
        unreadCountState = remember { mutableIntStateOf(1) },
        markAllRead = {},
        toggleRead = {},
    ) {}
}

@PreviewThemes
@Composable
fun PreviewEmptyState() = PreviewAppTheme {
    EmptyState(false)
}

