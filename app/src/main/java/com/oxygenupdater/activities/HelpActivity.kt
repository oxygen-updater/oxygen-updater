package com.oxygenupdater.activities

import com.oxygenupdater.R

class HelpActivity : SupportActionBarActivity(
    R.layout.activity_help,
    MainActivity.PAGE_ABOUT
) {
    companion object {
        const val TRANSITION_NAME = "HELP_TRANSITION"
    }
}
