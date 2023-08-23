package com.oxygenupdater.compose.activities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.PullRefresh
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberTypedCallback
import com.oxygenupdater.compose.ui.install.InstallGuideScreen
import com.oxygenupdater.compose.ui.install.InstallGuideViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallGuideActivity : ComposeSupportActionBarActivity(
    MainActivity.PAGE_UPDATE,
    R.string.install_guide,
) {

    private val viewModel by viewModel<InstallGuideViewModel>()

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()

        PullRefresh(state, rememberTypedCallback { it.isEmpty() }, rememberCallback(viewModel::refresh)) {
            InstallGuideScreen(state)
        }
    }
}
