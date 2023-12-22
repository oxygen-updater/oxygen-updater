package com.oxygenupdater.ui.news

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.LineHeightForTextStyle
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertSizeIsEqualTo
import com.oxygenupdater.get
import com.oxygenupdater.models.Article
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ErrorStateTestTag
import com.oxygenupdater.ui.common.ErrorState_TextTestTag
import com.oxygenupdater.ui.common.ErrorState_TitleTestTag
import com.oxygenupdater.ui.common.IconTextTestTag
import com.oxygenupdater.ui.common.OutlinedIconButtonTestTag
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.validateLazyColumn
import org.junit.Test

class NewsListScreenTest : ComposeBaseTest() {

    private val itemCount = PreviewNewsListData.size

    @Test
    fun newsListScreen_errorAllRead() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
            data = PreviewNewsListData.filter { it.readState },
        )

        // Banner — toggle to show only unread
        rule[IconTextTestTag].assertAndPerformClick()

        rule[ErrorState_TitleTestTag].assertHasTextExactly(R.string.news_empty_state_all_read_header)
        rule[ErrorState_TextTestTag].assertHasTextExactly(R.string.news_empty_state_all_read_text)

        rule[OutlinedIconButtonTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
    }

    @Test
    fun newsListScreen_errorEmpty() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
            data = listOf(),
        )

        rule[ErrorState_TitleTestTag].assertHasTextExactly(R.string.news_empty_state_none_available_header)
        rule[ErrorState_TextTestTag].assertHasTextExactly(R.string.news_empty_state_none_available_text)

        rule[OutlinedIconButtonTestTag].assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("onRefresh")

        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
    }

    @Test
    fun newsListScreen_bottomBar() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
        )

        common()

        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
        val rows = rule[NewsListScreen_LazyColumnTestTag, true].validateLazyColumn(itemCount)

        repeat(itemCount) { index ->
            val article = PreviewNewsListData[index]
            val node = rows[index]

            validateItemNodes(
                article = article,
                node = node,
                container = node.onChildren(),
                badgeSize = 6.dp,
                imageSize = 80.dp,
                forGrid = false,
            )
        }
    }

    @Test
    fun newsListScreen_sideRail() {
        setContent(
            navType = NavType.SideRail,
            windowWidthSize = WindowWidthSizeClass.Expanded,
            windowHeightSize = WindowHeightSizeClass.Expanded,
        )

        common()

        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        val items = rule[NewsListScreen_LazyVerticalGridTestTag, true].onChildren().run {
            filter(hasTestTag(NewsListScreen_ItemColumnTestTag)).assertCountEquals(itemCount)
        }

        repeat(itemCount) { index ->
            val article = PreviewNewsListData[index]
            val columnContainer = items[index].onChildren()

            validateItemNodes(
                article = article,
                node = columnContainer[0],
                container = columnContainer,
                badgeSize = 24.dp,
                imageSize = 224.dp,
                forGrid = true,
            )
        }
    }

    private fun common() {
        rule[ErrorStateTestTag].assertDoesNotExist()

        // Banner
        rule[IconTextTestTag].run {
            assertHasClickAction()
            performTouchInput { longClick() } // should show menu now
        }

        rule[NewsListScreen_MarkAllReadMenuTestTag].run {
            assertIsDisplayed()
            onParent().onParent().assert(isPopup())
            onChild().assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMarkAllReadClick")
            assertIsNotDisplayed()
        }
    }

    private fun validateItemNodes(
        article: Article,
        node: SemanticsNodeInteraction,
        container: SemanticsNodeInteractionCollection,
        badgeSize: Dp,
        imageSize: Dp,
        forGrid: Boolean,
    ) {
        val children = node.onChildren()

        val badge = children.filterToOne(hasTestTag(NewsListScreen_ItemBadgeTestTag))
        if (article.readState) badge.assertDoesNotExist() else badge.assertSizeIsEqualTo(badgeSize)

        // Image
        children.filterToOne(hasTestTag(NewsListScreen_ItemImageTestTag)).assertSizeIsEqualTo(imageSize)

        if (forGrid) {
            // Title & subtitle (using filterToOne because badge can be [1] depending on read status)
            children.filterToOne(hasTextExactly(article.title!!, article.subtitle!!)).assertExists()
        } else {
            // Title
            children.filterToOne(hasTextExactly(article.title!!)).fetchSemanticsNode().run {
                assertMaxLines(LineHeightForTextStyle.titleMedium, expectedMaxLines = 2)
            }
            // Subtitle
            children.filterToOne(hasTextExactly(article.subtitle!!)).fetchSemanticsNode().run {
                assertMaxLines(LineHeightForTextStyle.bodyMedium, expectedMaxLines = 2)
            }
        }

        // Timestamp & author
        container.filterToOne(hasTextExactly(article.getFooterText())).fetchSemanticsNode().run {
            assertMaxLines(LineHeightForTextStyle.bodySmall)
        }

        val menu = rule[NewsListScreen_ItemMenuTestTag]
        val button = container.filter(hasClickAction())[if (forGrid) 1 else 0]

        button.performClick() // should open menu now
        menu.onChildAt(0).assertAndPerformClick() // toggle read status
        ensureCallbackInvokedExactlyOnce("onToggleReadClick: ${article.id}")
        menu.assertDoesNotExist() // menu should be hidden now

        button.performClick() // re-open menu
        menu.onChildAt(3).assertAndPerformClick() // copy link
        assertCopiedToClipboard(article.webUrl)
        menu.assertDoesNotExist()

        // Finally, test if tapping on the item
        node.assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("openItem: ${article.id}")
        badge.assertDoesNotExist()
    }

    private fun setContent(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        windowHeightSize: WindowHeightSizeClass,
        data: List<Article> = PreviewNewsListData,
    ) = setContent {
        NewsListScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            windowHeightSize = windowHeightSize,
            state = RefreshAwareState(false, data),
            onRefresh = { trackCallback("onRefresh") },
            unreadCountState = remember { mutableIntStateOf(data.count { !it.readState }) },
            onMarkAllReadClick = { trackCallback("onMarkAllReadClick") },
            onToggleReadClick = { trackCallback("onToggleReadClick: ${it.id}") },
            openItem = { trackCallback("openItem: $it") },
        )
    }
}
