package com.oxygenupdater.ui.common

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
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
import org.junit.Test

class LazyVerticalGridTest : ComposeBaseTest() {

    private val items = arrayOf(
        GridItem(Symbols.Help, R.string.install_guide) {},
        GridItem(Symbols.Forum, R.string.faq_menu_item) {},
        GridItem(Logos.Discord, R.string.about_discord_button_text) {},
        GridItem(Symbols.Mail, R.string.about_email_button_text) {},
        GridItem(Logos.GitHub, R.string.about_github_button_text) {},
        GridItem(Symbols.Link, R.string.about_website_button_text) {},
        GridItem(Logos.Patreon, R.string.about_patreon_button_text) {},
        GridItem(Symbols.Star, R.string.about_rate_button_text) {},
    )

    private val columnCount = 2
    private val itemCount = items.size

    @Test
    fun lazyVerticalGrid() {
        setContent {
            LazyVerticalGrid(
                columnCount = columnCount,
                items = items,
            )
        }

        rule[LazyVerticalGridTestTag].run {
            // Height must be exact as according to the formula
            assertHeightIsEqualTo((ItemHeight * (itemCount / columnCount)).dp)

            // All children must be clickable
            onChildren().run {
                assertCountEquals(itemCount)
                assertAll(hasClickAction())
            }
        }
    }
}
