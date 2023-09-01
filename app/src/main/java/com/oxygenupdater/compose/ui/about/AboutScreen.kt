package com.oxygenupdater.compose.ui.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.oxygenupdater.R
import com.oxygenupdater.compose.activities.FaqActivity
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Discord
import com.oxygenupdater.compose.icons.Faq
import com.oxygenupdater.compose.icons.GitHub
import com.oxygenupdater.compose.icons.Patreon
import com.oxygenupdater.compose.ui.common.GridItem
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.RichText
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.extensions.openEmail
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.startActivity
import com.oxygenupdater.extensions.startInstallActivity
import com.oxygenupdater.extensions.withAppReferrer

@Composable
fun AboutScreen() = Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = 16.dp), // must be after `verticalScroll`
    Arrangement.spacedBy(16.dp)
) {
    Buttons()

    RichText(
        stringResource(R.string.about_description),
        Modifier.padding(horizontal = 16.dp),
    )

    RichText(
        stringResource(R.string.about_support),
        Modifier.padding(horizontal = 16.dp),
    )

    Text(
        stringResource(R.string.about_background_story_header),
        Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleMedium
    )

    RichText(
        stringResource(R.string.about_background_story),
        Modifier.padding(horizontal = 16.dp),
    )

    ItemDivider()

    Text(
        stringResource(R.string.about_third_party_app_notice),
        Modifier.padding(horizontal = 16.dp),
        MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun Buttons() = with(LocalContext.current) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        // 4 items of 56dp height each (32dp total vertical padding + 24dp icon)
        Modifier.height(224.dp),
        userScrollEnabled = false
    ) {
        item {
            GridItem(Icons.Rounded.HelpOutline, R.string.install_guide) { startInstallActivity(true) }
        }

        item {
            GridItem(CustomIcons.Faq, R.string.faq_menu_item) { startActivity<FaqActivity>() }
        }

        item {
            GridItem(CustomIcons.Discord, R.string.about_discord_button_text) { openLink(LinkType.Discord) }
        }

        item {
            GridItem(Icons.Rounded.MailOutline, R.string.about_email_button_text, ::openEmail)
        }

        item {
            GridItem(CustomIcons.GitHub, R.string.about_github_button_text) { openLink(LinkType.GitHub) }
        }

        item {
            GridItem(Icons.Rounded.Link, R.string.about_website_button_text) { openLink(LinkType.Website) }
        }

        item {
            GridItem(CustomIcons.Patreon, R.string.about_patreon_button_text) { openLink(LinkType.Patreon) }
        }

        item {
            GridItem(Icons.Rounded.StarOutline, R.string.about_rate_button_text, ::openPlayStorePage)
        }
    }
}

private fun Context.openLink(link: LinkType) = startActivity(
    Intent(Intent.ACTION_VIEW, link.value.toUri()).withAppReferrer(this)
)

@JvmInline
private value class LinkType(val value: String) {

    override fun toString() = value

    companion object {
        val Discord = LinkType("https://discord.gg/5TXdhKJ")
        val GitHub = LinkType("https://github.com/oxygen-updater/oxygen-updater")
        val Patreon = LinkType("https://patreon.com/oxygenupdater")
        val Website = LinkType("https://oxygenupdater.com/")
    }
}

@PreviewThemes
@Composable
fun PreviewAboutScreen() = PreviewAppTheme {
    AboutScreen()
}
