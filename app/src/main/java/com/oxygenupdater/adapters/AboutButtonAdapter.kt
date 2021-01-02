package com.oxygenupdater.adapters

import android.app.Activity
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openDiscord
import com.oxygenupdater.extensions.openEmail
import com.oxygenupdater.extensions.openGitHub
import com.oxygenupdater.extensions.openPatreon
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.openWebsite
import com.oxygenupdater.extensions.startFaqActivity
import com.oxygenupdater.extensions.startHelpActivity
import com.oxygenupdater.models.GridButton

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class AboutButtonAdapter(
    private val activity: Activity
) : GridButtonAdapter(
    arrayOf(
        GridButton(activity.getString(R.string.help), R.drawable.help),
        GridButton(activity.getString(R.string.faq_menu_item), R.drawable.faq),
        GridButton(activity.getString(R.string.about_discord_button_text), R.drawable.discord),
        GridButton(activity.getString(R.string.about_email_button_text), R.drawable.email),
        GridButton(activity.getString(R.string.about_github_button_text), R.drawable.github),
        GridButton(activity.getString(R.string.about_website_button_text), R.drawable.link),
        GridButton(activity.getString(R.string.about_patreon_button_text), R.drawable.patreon),
        GridButton(activity.getString(R.string.about_rate_button_text), R.drawable.rate),
    )
) {

    init {
        init { view, item ->
            when (item.drawableResId) {
                R.drawable.help -> activity.startHelpActivity(view)
                R.drawable.faq -> activity.startFaqActivity(view)
                R.drawable.discord -> activity.openDiscord()
                R.drawable.email -> activity.openEmail()
                R.drawable.github -> activity.openGitHub()
                R.drawable.link -> activity.openWebsite()
                R.drawable.patreon -> activity.openPatreon()
                R.drawable.rate -> activity.openPlayStorePage()
            }
        }
    }
}
