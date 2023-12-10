package com.oxygenupdater.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.oxygenupdater.R
import com.oxygenupdater.activities.FaqActivity
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.startActivity
import com.oxygenupdater.extensions.startInstallActivity
import com.oxygenupdater.extensions.withAppReferrer
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Discord
import com.oxygenupdater.icons.Faq
import com.oxygenupdater.icons.GitHub
import com.oxygenupdater.icons.Patreon
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.GridItem
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.LazyVerticalGrid
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize

@Composable
fun AboutScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    openEmail: () -> Unit,
) = Column(Modifier.verticalScroll(rememberScrollState())) {
    Buttons(windowWidthSize = windowWidthSize, openEmail = openEmail)

    RichText(
        text = stringResource(R.string.about_description),
        modifier = modifierDefaultPaddingStartTopEnd
    )

    RichText(
        text = stringResource(R.string.about_support),
        modifier = modifierDefaultPaddingStartTopEnd
    )

    Text(
        text = stringResource(R.string.about_background_story_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifierDefaultPaddingStartTopEnd
    )

    RichText(
        text = stringResource(R.string.about_background_story),
        modifier = modifierDefaultPaddingStartTopEnd
    )

    Spacer(Modifier.weight(1f))
    ItemDivider(Modifier.padding(vertical = 16.dp))

    Text(
        text = stringResource(R.string.about_third_party_app_notice),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    )

    ConditionalNavBarPadding(navType)
}

@Composable
private fun Buttons(
    windowWidthSize: WindowWidthSizeClass,
    openEmail: () -> Unit,
) = with(LocalContext.current) {
    LazyVerticalGrid(
        // We have 8 total items, so group evenly by 4 if we have enough space; otherwise 2
        columnCount = if (windowWidthSize == WindowWidthSizeClass.Expanded) 4 else 2,
        items = arrayOf(
            GridItem(Icons.AutoMirrored.Rounded.HelpOutline, R.string.install_guide) { startInstallActivity(true) },
            GridItem(CustomIcons.Faq, R.string.faq_menu_item) { startActivity<FaqActivity>() },
            GridItem(CustomIcons.Discord, R.string.about_discord_button_text) { openLink(LinkType.Discord) },
            GridItem(Icons.Rounded.MailOutline, R.string.about_email_button_text, openEmail),
            GridItem(CustomIcons.GitHub, R.string.about_github_button_text) { openLink(LinkType.GitHub) },
            GridItem(Icons.Rounded.Link, R.string.about_website_button_text) { openLink(LinkType.Website) },
            GridItem(CustomIcons.Patreon, R.string.about_patreon_button_text) { openLink(LinkType.Patreon) },
            GridItem(Icons.Rounded.StarOutline, R.string.about_rate_button_text, ::openPlayStorePage),
        ),
    )
}

private fun Context.openLink(link: LinkType) {
    val url = link.value
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).withAppReferrer(packageName))
    } catch (e: ActivityNotFoundException) {
        // Fallback: copy to clipboard instead
        copyToClipboard(url)
    }
}

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
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    AboutScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        openEmail = {},
    )
}
