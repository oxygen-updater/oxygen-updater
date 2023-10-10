package com.oxygenupdater.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.ZeroCornerSize
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
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.activities.NewsItemActivity
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Image
import com.oxygenupdater.icons.Info
import com.oxygenupdater.icons.NewsMultiple
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.DropdownMenuItem
import com.oxygenupdater.ui.common.ErrorState
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxSize
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberCallback
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.main.Screen
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsListScreen(
    navType: NavType,
    windowSize: WindowSizeClass,
    state: RefreshAwareState<List<NewsItem>>,
    refresh: () -> Unit,
    unreadCountState: MutableIntState,
    markAllRead: () -> Unit,
    toggleRead: (NewsItem) -> Unit,
    openItem: (id: Long) -> Unit,
) = PullRefresh(
    state = state,
    shouldShowProgressIndicator = { it.isEmpty() },
    onRefresh = refresh,
) {
    val refreshing = state.refreshing
    var onlyUnread by rememberSaveableState("onlyUnread", false)

    val data = if (onlyUnread) state.data.filterNot { it.readState } else state.data
    val list = if (!refreshing) rememberSaveable(onlyUnread) { data } else data

    LaunchedEffect(onlyUnread) {
        unreadCountState.intValue = if (onlyUnread) list.size else list.count { !it.readState }
    }

    val unreadCount by remember { unreadCountState }
    Screen.NewsList.badge = unreadCount.let { if (it == 0) null else it.toString() }

    Column {
        Banner(
            text = stringResource(
                if (onlyUnread) R.string.news_unread_count_2 else R.string.news_unread_count_1,
                unreadCount,
            ),
            onClick = { onlyUnread = !onlyUnread },
            onMarkAllReadClick = {
                markAllRead()
                unreadCountState.intValue = 0
            },
        )

        ItemDivider()

        if (onlyUnread && unreadCount == 0) ErrorState(
            navType = navType,
            titleResId = R.string.news_empty_state_all_read_header,
            icon = CustomIcons.NewsMultiple,
            textResId = R.string.news_empty_state_all_read_text,
            rich = false,
            refresh = null,
        ) else if (list.isEmpty()) ErrorState(
            navType = navType,
            titleResId = R.string.news_empty_state_none_available_header,
            icon = CustomIcons.NewsMultiple,
            textResId = R.string.news_empty_state_none_available_text,
            rich = false,
            refresh = refresh,
        ) else {
            val itemToggleRead: (NewsItem) -> Unit = {
                toggleRead(it)
                if (it.readState) unreadCountState.intValue++ else {
                    // Coerce to at least 0, just in case there's an inconsistency
                    if (unreadCountState.intValue > 0) unreadCountState.intValue--
                }
                it.readState = !it.readState
            }
            val itemOnClick: (NewsItem) -> Unit = {
                // Decrease unread count because we're making it read
                if (!it.readState) {
                    // Coerce to at least 0, just in case there's an inconsistency
                    if (unreadCountState.intValue > 0) unreadCountState.intValue--
                }
                it.readState = true

                NewsItemActivity.item = it
                openItem(it.id ?: NotSetL)
            }

            if (navType == NavType.BottomBar) {
                val size = DpSize(80.dp, 80.dp)
                LazyColumn(modifierMaxSize) {
                    items(items = list, key = { it.id ?: Random.nextLong() }) {
                        NewsListItem(
                            refreshing = refreshing,
                            item = it,
                            size = size,
                            toggleRead = { itemToggleRead(it) },
                            onClick = { itemOnClick(it) },
                        )
                    }
                }
            } else Column {
                val size = remember(windowSize) {
                    DpSize(
                        width = when (windowSize.widthSizeClass) {
                            WindowWidthSizeClass.Medium -> 192.dp
                            WindowWidthSizeClass.Expanded -> 224.dp
                            else -> 160.dp
                        },
                        height = when (windowSize.heightSizeClass) {
                            WindowHeightSizeClass.Medium -> 192.dp
                            WindowHeightSizeClass.Expanded -> 224.dp
                            else -> 128.dp
                        },
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.FixedSize(size.width),
                    state = rememberLazyGridState(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.windowInsetsPadding(
                        // Leave space for 2/3-button nav bar in landscape mode
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                    )
                ) {
                    items(items = list, key = { it.id ?: Random.nextLong() }) {
                        NewsGridItem(
                            refreshing = refreshing,
                            item = it,
                            size = size,
                            toggleRead = { itemToggleRead(it) },
                            onClick = { itemOnClick(it) },
                        )
                    }
                }

                ConditionalNavBarPadding(navType)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.NewsListItem(
    refreshing: Boolean,
    item: NewsItem,
    size: DpSize,
    toggleRead: () -> Unit,
    onClick: () -> Unit,
) = Column(
    Modifier
        .animateItemPlacement()
        .clickable(!refreshing, onClick = onClick)
) {
    Box {
        if (!refreshing && !item.readState) Badge(Modifier.offset(4.dp, 16.dp))

        Row(modifierDefaultPaddingStartTopEnd) {
            Column(
                Modifier
                    .weight(1f)
                    .requiredHeight(size.height) // same as image
                    .padding(end = 8.dp)
            ) {
                ListItemTitles(refreshing = refreshing, item = item)
            }

            NewsImage(
                refreshing = refreshing,
                item = item,
                size = size,
                modifier = Modifier.graphicsLayer {
                    if (refreshing) return@graphicsLayer
                    alpha = if (item.readState) 0.87f else 1f
                }
            )
        }
    }

    Footer(
        refreshing = refreshing,
        item = item,
        toggleRead = toggleRead,
        startPadding = 16.dp,
        menuXOffset = 6.dp, // bring inline with image
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyGridItemScope.NewsGridItem(
    refreshing: Boolean,
    item: NewsItem,
    size: DpSize,
    toggleRead: () -> Unit,
    onClick: () -> Unit,
) = Column(Modifier.animateItemPlacement()) {
    Box(
        Modifier
            .requiredSize(size)
            .clickable(!refreshing, onClick = onClick)
    ) {
        NewsImage(refreshing = refreshing, item = item, size = size)

        // Overlay over image
        Box(
            modifierMaxSize
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
        )

        if (!refreshing && !item.readState) GridItemBadge()

        GridItemTitles(refreshing = refreshing, item = item)
    }

    Footer(
        refreshing = refreshing,
        item = item,
        toggleRead = toggleRead,
        startPadding = 0.dp,
        menuXOffset = 22.dp, // bring inline with image
    )
}

@Composable
private fun GridItemBadge() {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val error = MaterialTheme.colorScheme.error
    Spacer(
        Modifier
            .size(24.dp)
            .clip(
                // Clip only top-left
                MaterialTheme.shapes.small.copy(
                    topEnd = ZeroCornerSize,
                    bottomEnd = ZeroCornerSize,
                    bottomStart = ZeroCornerSize,
                )
            )
            .drawWithCache {
                // Top-left triangle
                val path = Path()
                path.moveTo(0f, 0f)
                if (ltr) {
                    path.lineTo(this.size.width, 0f)
                    path.lineTo(0f, this.size.height)
                } else {
                    path.lineTo(this.size.width, this.size.height)
                    path.lineTo(this.size.width, 0f)
                }
                path.close()
                onDrawBehind {
                    drawPath(path, error)
                }
            }
    )
}

/**
 * Draws max 2 [title][NewsItem.title] & [subtitle][NewsItem.subtitle] lines, with the latter being
 * auto-resized to fit available space (from 12sp to 14sp).
 *
 * Even though [GridItemTitles] text measurer logic is superior (max title lines with min 1 subtitle line),
 * we don't use it here because it's important to show refresh placeholders for title & subtitle separately,
 * which isn't possible if drawing both together in the same element.
 */
@Composable
private fun ListItemTitles(refreshing: Boolean, item: NewsItem) {
    val titleMedium = MaterialTheme.typography.titleMedium

    Text(
        text = item.title ?: "Unknown title",
        overflow = TextOverflow.Ellipsis, maxLines = 2,
        style = titleMedium,
        modifier = Modifier
            .withPlaceholder(refreshing, titleMedium)
            .graphicsLayer {
                if (refreshing) return@graphicsLayer
                alpha = if (item.readState) 0.7f else 1f
            }
    )

    AutoresizeText(
        text = item.subtitle ?: "",
        fontSizeRange = FontSizeRange(12f, 14f),
        modifier = Modifier.withPlaceholder(refreshing)
    )
}

/**
 * This draws text via [rememberTextMeasurer] into [Spacer] to maximizes [title][NewsItem.title] lines
 * while also ensuring at least 1 line for [subtitle][NewsItem.subtitle].
 *
 * Because of how grid items are laid out (titles are drawn over a "background" image) we skip drawing
 * titles when UI is refreshing, because image's [withPlaceholder] is already enough to signal refresh status.
 */
@Composable
private fun GridItemTitles(refreshing: Boolean, item: NewsItem) {
    if (refreshing) return // don't draw titles for grid items if refreshing

    val titleMedium = MaterialTheme.typography.titleMedium
    val bodyMedium = MaterialTheme.typography.bodyMedium

    val currentColor = LocalContentColor.current
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    val spaceForOneSubtitleLine = LocalDensity.current.run {
        val subtitleLineHeight = bodyMedium.lineHeight.value
        val titleFontPadding = abs(titleMedium.lineHeight.value - titleMedium.fontSize.value) / 2
        (subtitleLineHeight + titleFontPadding).sp.roundToPx()
    }

    Spacer(
        modifierMaxSize
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .drawWithCache {
                val width = size.width.toInt()
                val height = size.height.toInt()

                val title = textMeasurer.measure(
                    text = item.title ?: "Unknown title",
                    // Leave space for min 1 subtitle line
                    constraints = Constraints(maxWidth = width, maxHeight = height - spaceForOneSubtitleLine),
                    overflow = TextOverflow.Ellipsis,
                    style = titleMedium,
                )

                val titleHeight = title.size.height
                val subtitle = textMeasurer.measure(
                    text = item.subtitle ?: "",
                    // Limit to remaining space
                    constraints = Constraints(maxWidth = width, maxHeight = height - titleHeight),
                    overflow = TextOverflow.Ellipsis,
                    style = bodyMedium,
                )

                onDrawBehind {
                    drawText(title, currentColor)
                    drawText(subtitle, onSurfaceVariant, Offset(0f, titleHeight.toFloat())) // position just below title
                }
            }
    )
}

@Composable
private fun NewsImage(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    item: NewsItem,
    size: DpSize,
) {
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

    AsyncImage(
        model = item.imageUrl?.let {
            val context = LocalContext.current
            val sizePx = LocalDensity.current.run { size.width.roundToPx() to size.height.roundToPx() }
            remember(it, sizePx) {
                ImageRequest.Builder(context)
                    .data(it)
                    .size(sizePx.first, sizePx.second)
                    .build()
            }
        },
        contentDescription = stringResource(R.string.icon),
        placeholder = defaultImage,
        error = defaultImage,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .requiredSize(size)
            .clip(MaterialTheme.shapes.small)
            .withPlaceholder(refreshing)
            .then(modifier)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Footer(
    refreshing: Boolean,
    item: NewsItem,
    toggleRead: () -> Unit,
    startPadding: Dp,
    menuXOffset: Dp,
) = Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifierMaxWidth.padding(start = startPadding)
) {
    val bodySmall = MaterialTheme.typography.bodySmall
    val authorName = item.authorName ?: "Unknown Author"
    Text(
        text = item.epochMilli?.let {
            DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            )
        }?.let {
            "$it \u2022 $authorName"
        } ?: authorName,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        style = bodySmall,
        modifier = Modifier
            .weight(1f, false)
            .basicMarquee()
            .withPlaceholder(refreshing, bodySmall)
    )

    ItemMenuOpener(
        refreshing = refreshing,
        item = item,
        toggleRead = toggleRead,
        xOffset = menuXOffset,
    )
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
        icon = CustomIcons.Info,
        text = text,
        modifier = modifierMaxWidth
            .combinedClickable(onLongClick = { showMenu = true }, onClick = onClick)
            .then(modifierDefaultPadding) // must be after `clickable`
    ) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(24.dp, 0.dp),
        ) {
            DropdownMenuItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                textResId = R.string.news_mark_all_read,
                onClick = {
                    onMarkAllReadClick()
                    showMenu = false
                },
            )
        }
    }
}

@Composable
private fun ItemMenuOpener(
    refreshing: Boolean,
    item: NewsItem,
    toggleRead: () -> Unit,
    xOffset: Dp,
) = Box {
    var showMenu by rememberSaveableState("showItemMenu", false)
    ItemMenu(showMenu, { showMenu = false }, item, toggleRead)

    IconButton(
        onClick = { showMenu = true },
        enabled = !refreshing,
        modifier = Modifier.offset(xOffset)
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.icon),
            tint = if (refreshing) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = itemMenuIconModifier
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
) = DropdownMenu(expanded, onDismiss) {
    DropdownMenuItem(
        icon = if (item.readState) Icons.Rounded.HighlightOff else Icons.Rounded.CheckCircleOutline,
        textResId = if (item.readState) R.string.news_mark_unread else R.string.news_mark_read,
        onClick = {
            onToggleReadClick()
            onDismiss()
        },
    )

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    DropdownMenuItem(
        icon = Icons.Rounded.OpenInBrowser,
        textResId = androidx.browser.R.string.fallback_menu_item_open_in_browser,
        onClick = rememberCallback(context, customTabIntent) {
            customTabIntent.launch(context, item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Icons.Outlined.Share,
        textResId = androidx.browser.R.string.fallback_menu_item_share_link,
        onClick = rememberCallback(context) {
            context.shareExternally(item.title ?: "", item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Icons.Rounded.Link,
        textResId = androidx.browser.R.string.fallback_menu_item_copy_link,
        onClick = rememberCallback(context) {
            context.copyToClipboard(item.webUrl)
            onDismiss()
        },
    )
}

// TODO(compose/news): switch to first-party solution when it's out: https://developer.android.com/jetpack/androidx/compose-roadmap#core-libraries
/**
 * @see <a href="https://stackoverflow.com/a/69780826">stackoverflow.com/a/69780826<a>
 */
@Composable
private fun AutoresizeText(
    text: String,
    fontSizeRange: FontSizeRange,
    modifier: Modifier,
    maxLines: Int = 2,
) {
    val (min, max, step) = fontSizeRange
    var fontSizeValue by remember { mutableFloatStateOf(max) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
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
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        }
    )
}

@Immutable
private data class FontSizeRange(
    val min: Float,
    val max: Float,
    val step: Float = DefaultStep,
) {
    init {
        require(min <= max) { "min should be less than or equal to max, $this" }
        require(step > 0) { "step should be greater than 0, $this" }
    }

    companion object {
        private const val DefaultStep = 1f
    }
}

// Perf: re-use common modifiers to avoid recreating the same object repeatedly
private val itemMenuIconModifier = Modifier.requiredSize(20.dp)

@PreviewThemes
@Composable
fun PreviewNewsListScreen() = PreviewAppTheme {
    val now = LocalDateTime.now()
    val long = "Unnecessarily long text, to get an accurate understanding of how its rendered"
    val windowSize = PreviewWindowSize
    NewsListScreen(
        navType = NavType.from(windowSize.widthSizeClass),
        windowSize = windowSize,
        state = RefreshAwareState(
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
        openItem = {},
    )
}
