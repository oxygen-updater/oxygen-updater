package com.oxygenupdater.ui.about

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openLink
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.icons.Discord
import com.oxygenupdater.icons.Forum
import com.oxygenupdater.icons.GitHub
import com.oxygenupdater.icons.Help
import com.oxygenupdater.icons.Link
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.Mail
import com.oxygenupdater.icons.Patreon
import com.oxygenupdater.icons.Star
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.GridItem
import com.oxygenupdater.ui.common.LazyVerticalGrid
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.main.ChildScreen
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize

@Composable
fun AboutScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    navigateTo: (ChildScreen) -> Unit,
    openEmail: () -> Unit,
) = Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .testTag(AboutScreenTestTag)
) {
    Buttons(
        // We have 8 total items, so group evenly by 4 if we have enough space; otherwise 2
        columnCount = if (windowWidthSize == WindowWidthSizeClass.Expanded) 4 else 2,
        navigateTo = navigateTo,
        openEmail = openEmail,
    )

    RichText(
        text = stringResource(R.string.about_description),
        modifier = modifierDefaultPaddingStartTopEnd.testTag(AboutScreen_DescriptionTestTag)
    )

    RichText(
        text = stringResource(R.string.about_support),
        modifier = modifierDefaultPaddingStartTopEnd.testTag(AboutScreen_SupportTestTag)
    )

    Text(
        text = stringResource(R.string.about_background_story_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifierDefaultPaddingStartTopEnd.testTag(AboutScreen_BackgroundStoryHeaderTestTag)
    )

    RichText(
        text = stringResource(R.string.about_background_story),
        modifier = modifierDefaultPaddingStartTopEnd.testTag(AboutScreen_BackgroundStoryTestTag)
    )

    Spacer(Modifier.weight(1f))
    HorizontalDivider(Modifier.padding(vertical = 16.dp))

    Text(
        text = stringResource(R.string.about_third_party_app_notice),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag(AboutScreen_ThirdPartyNoticeTestTag)
    )

    ConditionalNavBarPadding(navType)
}

@Composable
private fun Buttons(
    columnCount: Int,
    navigateTo: (ChildScreen) -> Unit,
    openEmail: () -> Unit,
) = with(LocalContext.current) {
    LazyVerticalGrid(
        columnCount = columnCount,
        items = arrayOf(
            GridItem(Symbols.Help, R.string.install_guide) {
                navigateTo(ChildScreen.Guide)
            },
            GridItem(Symbols.Forum, R.string.faq_menu_item) {
                navigateTo(ChildScreen.Faq)
            },
            GridItem(Logos.Discord, R.string.about_discord_button_text) { openLink(LinkType.Discord.value) },
            GridItem(Symbols.Mail, R.string.about_email_button_text, openEmail),
            GridItem(Logos.GitHub, R.string.about_github_button_text) { openLink(LinkType.GitHub.value) },
            GridItem(Symbols.Link, R.string.about_website_button_text) { openLink(LinkType.Website.value) },
            GridItem(Logos.Patreon, R.string.about_patreon_button_text) { openLink(LinkType.Patreon.value) },
            GridItem(Symbols.Star, R.string.about_rate_button_text, ::openPlayStorePage),
        ),
    )
}

@JvmInline
private value class LinkType private constructor(val value: String) {

    override fun toString() = value

    companion object {
        val Discord = LinkType("https://discord.gg/5TXdhKJ")
        val GitHub = LinkType("https://github.com/oxygen-updater/oxygen-updater")
        val Patreon = LinkType("https://patreon.com/oxygenupdater")
        val Website = LinkType("https://oxygenupdater.com/")
    }
}

private const val TAG = "AboutScreen"

@VisibleForTesting
const val AboutScreenTestTag = TAG

@VisibleForTesting
const val AboutScreen_DescriptionTestTag = TAG + "_Description"

@VisibleForTesting
const val AboutScreen_SupportTestTag = TAG + "_Support"

@VisibleForTesting
const val AboutScreen_BackgroundStoryHeaderTestTag = TAG + "_BackgroundStoryHeader"

@VisibleForTesting
const val AboutScreen_BackgroundStoryTestTag = TAG + "_BackgroundStory"

@VisibleForTesting
const val AboutScreen_ThirdPartyNoticeTestTag = TAG + "_ThirdPartyNotice"

@PreviewThemes
@Composable
fun PreviewAboutScreen() = PreviewAppTheme {
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    AboutScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        navigateTo = {},
        openEmail = {},
    )
}
