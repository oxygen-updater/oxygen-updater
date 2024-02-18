package com.oxygenupdater.ui.news

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.get
import com.oxygenupdater.utils.AppUserAgent
import org.junit.Test

class WebViewTest : ComposeBaseTest() {

    @Test
    fun webView() {
        lateinit var state: WebViewState
        setContent {
            state = rememberWebViewState()
            WebView(state)
        }

        rule[WebViewTestTag].assertExists()

        val view = state.webView
        assert(view != null) { "WebView not initialized" }

        view!!.layoutParams.let {
            assert(it is FrameLayout.LayoutParams) { "LayoutParams must be of type ${FrameLayout.LayoutParams::class}" }
            assert(it.width == MATCH_PARENT) {
                "LayoutParams.width did not match. Expected: MATCH_PARENT, actual: ${it.width}."
            }
        }

        assert(view.background == null) { "Background must be transparent" }

        rule.runOnUiThread { view.settings }.run {
            assert(javaScriptEnabled) { "JavaScript must be enabled" }
            assert(userAgentString.startsWith(AppUserAgent)) { "User-Agent must start with $AppUserAgent" }
        }
    }
}
