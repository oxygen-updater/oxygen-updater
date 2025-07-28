package com.oxygenupdater.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.oxygenupdater.R
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.icons.Cancel
import com.oxygenupdater.icons.CheckCircle
import com.oxygenupdater.icons.FullCoverage
import com.oxygenupdater.icons.ImageFilled
import com.oxygenupdater.icons.Info
import com.oxygenupdater.icons.Link
import com.oxygenupdater.icons.MoreVert
import com.oxygenupdater.icons.OpenInBrowser
import com.oxygenupdater.icons.PlaylistAddCheck
import com.oxygenupdater.icons.Share
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.models.Article
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.DropdownMenuItem
import com.oxygenupdater.ui.common.ErrorState
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxSize
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.common.scrollbar.Scrollbar
import com.oxygenupdater.ui.common.scrollbar.rememberDraggableScroller
import com.oxygenupdater.ui.common.scrollbar.scrollbarState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.main.Screen
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun NewsListScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    windowHeightSize: WindowHeightSizeClass,
    state: RefreshAwareState<List<Article>>,
    onRefresh: () -> Unit,
    unreadCountState: MutableIntState,
    onMarkAllReadClick: () -> Unit,
    onToggleReadClick: (Article) -> Unit,
    openItem: (id: Long) -> Unit,
) = PullRefresh(
    state = state,
    shouldShowProgressIndicator = { it.isEmpty() },
    onRefresh = onRefresh,
) {
    val refreshing = state.refreshing
    var onlyUnread by rememberState(false)

    val data = if (onlyUnread) state.data.filterNot { it.readState } else state.data
    val list = if (!refreshing) rememberSaveable(data) { data } else data

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
                onMarkAllReadClick()
                unreadCountState.intValue = 0
            },
        )

        HorizontalDivider()

        if (onlyUnread && unreadCount == 0) ErrorState(
            navType = navType,
            titleResId = R.string.news_empty_state_all_read_header,
            icon = Symbols.FullCoverage,
            textResId = R.string.news_empty_state_all_read_text,
            rich = false,
            onRefreshClick = null,
        ) else if (list.isEmpty()) ErrorState(
            navType = navType,
            titleResId = R.string.news_empty_state_none_available_header,
            icon = Symbols.FullCoverage,
            textResId = R.string.news_empty_state_none_available_text,
            rich = false,
            onRefreshClick = onRefresh,
        ) else {
            val onItemToggleRead: (Article) -> Unit = {
                onToggleReadClick(it)
                if (it.readState) unreadCountState.intValue++ else {
                    // Coerce to at least 0, just in case there's an inconsistency
                    if (unreadCountState.intValue > 0) unreadCountState.intValue--
                }
                it.readState = !it.readState
            }
            val onItemClick: (Article) -> Unit = {
                ArticleViewModel.item = it
                openItem(it.id ?: NotSetL)
            }

            if (navType == NavType.BottomBar) {
                val listState = rememberLazyListState()
                val size = DpSize(80.dp, 80.dp)

                // Run when we have new articles, by keying the first item's ID.
                // This should be more performant than using the list itself as the key.
                LaunchedEffect(list.firstOrNull()?.id) {
                    // Scroll to the first item upon refresh
                    if (listState.firstVisibleItemIndex > 0) try {
                        listState.animateScrollToItem(0)
                    } catch (_: Exception) {
                        try {
                            listState.scrollToItem(0)
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }

                Box {
                    LazyColumn(
                        state = listState,
                        modifier = modifierMaxSize.testTag(NewsListScreen_LazyColumnTestTag)
                    ) {
                        items(items = list, key = { it.id ?: Random.nextLong() }) {
                            NewsListItem(
                                refreshing = refreshing,
                                item = it,
                                size = size,
                                onToggleReadClick = { onItemToggleRead(it) },
                                onClick = { onItemClick(it) },
                            )
                        }
                    }

                    // Must be at the end — highest in the Z-hierarchy => nothing draws above it,
                    // and user can press/drag for fast scrolling.
                    // TODO(compose/news): switch to first-party solution when it's out: https://developer.android.com/jetpack/androidx/compose-roadmap
                    val numItems = list.size
                    listState.Scrollbar(
                        state = listState.scrollbarState(numItems),
                        onThumbMoved = listState.rememberDraggableScroller(numItems),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            } else Column {
                val gridState = rememberLazyGridState()
                val size = remember(windowWidthSize, windowHeightSize) {
                    DpSize(
                        width = when (windowWidthSize) {
                            WindowWidthSizeClass.Medium -> 192.dp
                            WindowWidthSizeClass.Expanded -> 224.dp
                            else -> 160.dp
                        },
                        height = when (windowHeightSize) {
                            WindowHeightSizeClass.Medium -> 192.dp
                            WindowHeightSizeClass.Expanded -> 224.dp
                            else -> 128.dp
                        },
                    )
                }

                // Run when we have new articles, by keying the first item's ID.
                // This should be more performant than using the list itself as the key.
                LaunchedEffect(list.firstOrNull()?.id) {
                    // Scroll to the first item upon refresh
                    if (gridState.firstVisibleItemIndex > 0) try {
                        gridState.animateScrollToItem(0)
                    } catch (_: Exception) {
                        try {
                            gridState.scrollToItem(0)
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }

                Box {
                    LazyVerticalGrid(
                        columns = GridCells.FixedSize(size.width),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            // Leave space for 2/3-button nav bar in landscape mode
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .testTag(NewsListScreen_LazyVerticalGridTestTag)
                    ) {
                        items(items = list, key = { it.id ?: Random.nextLong() }) {
                            NewsGridItem(
                                refreshing = refreshing,
                                item = it,
                                size = size,
                                onToggleReadClick = { onItemToggleRead(it) },
                                onClick = { onItemClick(it) },
                            )
                        }
                    }

                    // Must be at the end — highest in the Z-hierarchy => nothing draws above it,
                    // and user can press/drag for fast scrolling.
                    // TODO(compose/news): switch to first-party solution when it's out: https://developer.android.com/jetpack/androidx/compose-roadmap
                    val numItems = list.size
                    gridState.Scrollbar(
                        state = gridState.scrollbarState(numItems),
                        onThumbMoved = gridState.rememberDraggableScroller(numItems),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }

                ConditionalNavBarPadding(navType)
            }
        }
    }
}

@Composable
private fun LazyItemScope.NewsListItem(
    refreshing: Boolean,
    item: Article,
    size: DpSize,
    onToggleReadClick: () -> Unit,
    onClick: () -> Unit,
) = Column(
    Modifier
        .animateItem(fadeInSpec = null, fadeOutSpec = null)
        .clickable(!refreshing, onClick = onClick)
        .testTag(NewsListScreen_ItemColumnTestTag)
) {
    Box {
        if (!refreshing && !item.readState) Badge(
            Modifier
                .offset(4.dp, 16.dp)
                .testTag(NewsListScreen_ItemBadgeTestTag)
        )

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
        onToggleReadClick = onToggleReadClick,
        startPadding = 16.dp,
        menuXOffset = 6.dp, // bring inline with image
    )
}

@Composable
private fun LazyGridItemScope.NewsGridItem(
    refreshing: Boolean,
    item: Article,
    size: DpSize,
    onToggleReadClick: () -> Unit,
    onClick: () -> Unit,
) = Column(
    Modifier
        .animateItem(fadeInSpec = null, fadeOutSpec = null)
        .testTag(NewsListScreen_ItemColumnTestTag)
) {
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
        onToggleReadClick = onToggleReadClick,
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
                val width = size.width
                val height = size.height

                path.moveTo(0f, 0f)
                if (ltr) {
                    path.lineTo(width, 0f)
                    path.lineTo(0f, height)
                } else {
                    path.lineTo(width, height)
                    path.lineTo(width, 0f)
                }
                path.close()

                onDrawBehind { drawPath(path, error) }
            }
            .testTag(NewsListScreen_ItemBadgeTestTag)
    )
}

/**
 * Draws max 2 [title][Article.title] & [subtitle][Article.subtitle] lines, with the latter being
 * auto-resized to fit available space (from 12sp to 14sp).
 *
 * Even though [GridItemTitles] text measurer logic is superior (max title lines with min 1 subtitle line),
 * we don't use it here because it's important to show refresh placeholders for title & subtitle separately,
 * which isn't possible if drawing both together in the same element.
 */
@Composable
private fun ListItemTitles(refreshing: Boolean, item: Article) {
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

    BasicText(
        text = item.subtitle ?: "",
        overflow = TextOverflow.Ellipsis, maxLines = 2,
        style = MaterialTheme.typography.bodyMedium.merge(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        autoSize = TextAutoSize.StepBased(
            minFontSize = 12.sp,
            maxFontSize = 14.sp,
        ),
        modifier = Modifier.withPlaceholder(refreshing)
    )
}

/**
 * This draws text via [rememberTextMeasurer] into [Spacer] to maximize [title][Article.title] lines
 * while also ensuring at least 1 line for [subtitle][Article.subtitle].
 *
 * Because of how grid items are laid out (titles are drawn over a "background" image) we skip drawing
 * titles when UI is refreshing, because image's [withPlaceholder] is already enough to signal refresh status.
 */
@Composable
private fun GridItemTitles(refreshing: Boolean, item: Article) {
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

    val titleStr = item.title ?: "Unknown title"
    val subtitleStr = item.subtitle ?: ""
    Spacer(
        modifierMaxSize
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .drawWithCache {
                val width = size.width.toInt()
                val height = size.height.toInt()

                val title = textMeasurer.measure(
                    text = titleStr,
                    // Leave space for min 1 subtitle line
                    constraints = Constraints(maxWidth = width, maxHeight = height - spaceForOneSubtitleLine),
                    overflow = TextOverflow.Ellipsis,
                    style = titleMedium,
                )

                val titleHeight = title.size.height
                val subtitle = textMeasurer.measure(
                    text = subtitleStr,
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
            // We need to explicitly set text semantics here, because we're drawing it ourselves
            .semantics {
                set(
                    SemanticsProperties.Text,
                    listOf(
                        AnnotatedString(item.title ?: "Unknown title"),
                        AnnotatedString(item.subtitle ?: ""),
                    )
                )
            }
    )
}

@Composable
private fun NewsImage(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    item: Article,
    size: DpSize,
) {
    val defaultImage = Symbols.ImageFilled.run {
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
            .then(modifier.testTag(NewsListScreen_ItemImageTestTag))
    )
}

@Composable
private fun Footer(
    refreshing: Boolean,
    item: Article,
    onToggleReadClick: () -> Unit,
    startPadding: Dp,
    menuXOffset: Dp,
) = Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifierMaxWidth.padding(start = startPadding)
) {
    val bodySmall = MaterialTheme.typography.bodySmall
    Text(
        text = item.getFooterText(),
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
        onToggleReadClick = onToggleReadClick,
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
    var showMenu by rememberState(false)
    IconText(
        icon = Symbols.Info,
        text = text,
        modifier = modifierMaxWidth
            .combinedClickable(onLongClick = { showMenu = true }, onClick = onClick)
            .then(modifierDefaultPadding) // must be after `clickable`
    ) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(24.dp, 0.dp),
            modifier = Modifier.testTag(NewsListScreen_MarkAllReadMenuTestTag)
        ) {
            DropdownMenuItem(
                icon = Symbols.PlaylistAddCheck,
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
    item: Article,
    onToggleReadClick: () -> Unit,
    xOffset: Dp,
) = Box {
    var showMenu by rememberState(false)
    ItemMenu(showMenu, { showMenu = false }, item, onToggleReadClick)

    IconButton(
        onClick = { showMenu = true },
        enabled = !refreshing,
        modifier = Modifier.offset(xOffset)
    ) {
        Icon(
            imageVector = Symbols.MoreVert, contentDescription = stringResource(R.string.icon),
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
    item: Article,
    onToggleReadClick: () -> Unit,
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag(NewsListScreen_ItemMenuTestTag)
) {
    DropdownMenuItem(
        icon = if (item.readState) Symbols.Cancel else Symbols.CheckCircle,
        textResId = if (item.readState) R.string.news_mark_unread else R.string.news_mark_read,
        onClick = {
            onToggleReadClick()
            onDismiss()
        },
    )

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    DropdownMenuItem(
        icon = Symbols.OpenInBrowser,
        textResId = androidx.browser.R.string.fallback_menu_item_open_in_browser,
        onClick = {
            customTabIntent.launch(context, item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Symbols.Share,
        textResId = androidx.browser.R.string.fallback_menu_item_share_link,
        onClick = {
            context.shareExternally(item.title ?: "", item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Symbols.Link,
        textResId = androidx.browser.R.string.fallback_menu_item_copy_link,
        onClick = {
            context.copyToClipboard(item.webUrl)
            onDismiss()
        },
    )
}

// Perf: re-use common modifiers to avoid recreating the same object repeatedly
private val itemMenuIconModifier = Modifier.requiredSize(20.dp)

@VisibleForTesting
fun Article.getFooterText(): String {
    val authorName = authorName ?: "Unknown Author"
    return epochMilli?.let {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        )
    }?.let {
        "$it • $authorName"
    } ?: authorName
}

private const val TAG = "NewsListScreen"

@VisibleForTesting
const val NewsListScreen_MarkAllReadMenuTestTag = TAG + "_MarkAllReadMenu"

@VisibleForTesting
const val NewsListScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val NewsListScreen_LazyVerticalGridTestTag = TAG + "_LazyVerticalGrid"

@VisibleForTesting
const val NewsListScreen_ItemColumnTestTag = TAG + "_ItemColumn"

@VisibleForTesting
const val NewsListScreen_ItemBadgeTestTag = TAG + "_ItemBadge"

@VisibleForTesting
const val NewsListScreen_ItemImageTestTag = TAG + "_ItemImage"

@VisibleForTesting
const val NewsListScreen_ItemMenuTestTag = TAG + "_ItemMenu"

@VisibleForTesting
val PreviewNewsListData = LocalDateTime.now().let { now ->
    val subtitle = "Unnecessarily long subtitle, to get an accurate understanding of how its rendered"
    listOf(
        Article(
            id = 1,
            title = "Oxygen Updater",
            subtitle = subtitle,
            imageUrl = "https://github.com/oxygen-updater.png",
            text = "",
            datePublished = now.minusDays(1).toString(),
            dateLastEdited = now.minusHours(4).toString(),
            authorName = "Author",
            read = true,
        ),
        Article(
            id = 2,
            title = "An unnecessarily long article title, to get an accurate understanding of how long titles are rendered. This line has a length of 255 characters and should provide enough information to tweak the AppBar UI for the best balance between readability & design.",
            subtitle = subtitle,
            imageUrl = "https://github.com/oxygen-updater.png",
            text = "",
            datePublished = now.minusDays(2).toString(),
            dateLastEdited = now.minusHours(5).toString(),
            authorName = "Author",
            read = false,
        ),
    )
}

@PreviewThemes
@Composable
fun PreviewNewsListScreen() = PreviewAppTheme {
    val windowSize = PreviewWindowSize
    NewsListScreen(
        navType = NavType.from(windowSize.widthSizeClass),
        windowWidthSize = windowSize.widthSizeClass,
        windowHeightSize = windowSize.heightSizeClass,
        state = RefreshAwareState(false, PreviewNewsListData),
        onRefresh = {},
        unreadCountState = rememberState(1),
        onMarkAllReadClick = {},
        onToggleReadClick = {},
        openItem = {},
    )
}
