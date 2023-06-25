package com.oxygenupdater.compose.activities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.PullRefresh
import com.oxygenupdater.compose.ui.faq.FaqScreen
import com.oxygenupdater.compose.ui.faq.FaqViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqActivity : ComposeSupportActionBarActivity(
    MainActivity.PAGE_ABOUT,
    R.string.faq,
) {

    private val viewModel by viewModel<FaqViewModel>()

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()

        PullRefresh(state, shouldShowProgressIndicator = {
            it.isEmpty()
        }, onRefresh = {
            viewModel.refresh()
        }) {
            FaqScreen(state)
        }
    }

    companion object {
        const val TRANSITION_NAME = "FAQ_TRANSITION"
    }
}
