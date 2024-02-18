package com.oxygenupdater.ui.common

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import com.oxygenupdater.BuildConfig.SERVER_DOMAIN
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.get
import org.junit.Test

class RichTextTest : ComposeBaseTest() {

    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            trackCallback("openUri: $uri")
        }
    }

    @Test
    fun richText_html() = common(
        """<a href="$SERVER_DOMAIN">$SERVER_DOMAIN</a>""",
        RichTextType.Html,
    )

    @Test
    fun richText_markdown() = common(
        """[$SERVER_DOMAIN]($SERVER_DOMAIN)""",
        RichTextType.Markdown,
    )

    private fun common(text: String, type: RichTextType) {
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                RichText(text = text, type = type)
            }
        }

        rule[RichText_ContainerTestTag].run {
            performTouchInput {
                longClick() // select text
                click()     // should open URI
            }

            ensureCallbackInvokedExactlyOnce("openUri: $SERVER_DOMAIN")
        }
    }
}
