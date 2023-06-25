package com.oxygenupdater.compose.activities

import androidx.compose.runtime.Composable
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.help.HelpScreen

class HelpActivity : ComposeSupportActionBarActivity(
    MainActivity.PAGE_ABOUT,
    R.string.help,
) {

    @Composable
    override fun Content() = HelpScreen()

    companion object {
        const val TRANSITION_NAME = "HELP_TRANSITION"
    }
}
