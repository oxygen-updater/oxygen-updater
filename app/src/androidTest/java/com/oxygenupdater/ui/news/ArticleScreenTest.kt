package com.oxygenupdater.ui.news

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.IconTextTestTag
import org.junit.Test

class ArticleScreenTest : ComposeBaseTest() {

    @Test
    fun articleScreen() {
        val webViewState = WebViewState()
        setContent {
            ArticleScreen(
                state = RefreshAwareState(false, PreviewArticleData),
                onRefresh = {},
                webViewState = webViewState,
                onLoadFinished = { trackCallback("onLoadFinished: ${it.id}") },
                modifier = Modifier
            )
        }

        rule[WebViewTestTag].assertExists()

        val webViewIdlingResource = object : IdlingResource {
            override val isIdleNow: Boolean
                get() = webViewState.loadingState == LoadingState.Finished

            override fun getDiagnosticMessageIfBusy() = "WebView hasn't finished loading yet"
        }

        rule.registerIdlingResource(webViewIdlingResource)
        rule.runOnIdle {
            ensureCallbackInvokedExactlyOnce("onLoadFinished: ${PreviewArticleData.id}")
        }
        rule.unregisterIdlingResource(webViewIdlingResource)

        // Note: first child is tested last, because it's the "Share link" button, and
        // testing it requires clicking. That would leave the app, preventing further testing.
        rule.onAllNodesWithTag(IconTextTestTag).run {
            assertCountEquals(4)

            get(3).onChildAt(1).assertHasTextExactly(PreviewArticleData.getRelativeTime())
            get(2).onChildAt(1).assertHasTextExactly(PreviewArticleData.subtitle)
            get(1).run {
                assertHasTextExactly(androidx.browser.R.string.fallback_menu_item_copy_link)
                assertAndPerformClick()
                assertCopiedToClipboard(PreviewArticleData.webUrl)
            }
            get(0).run {
                assertHasTextExactly(androidx.browser.R.string.fallback_menu_item_share_link)
                assertAndPerformClick() // should open share sheet
                try {
                    assertIsNotFocused()
                } catch (e: IllegalStateException) {
                    // This is likely "No compose hierarchies found in the app", which is
                    // fine because that's what we expect (we're in the share sheet now).
                }
            }
        }
    }
}
