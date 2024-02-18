package com.oxygenupdater.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Discord
import com.oxygenupdater.icons.Faq
import com.oxygenupdater.icons.GitHub
import com.oxygenupdater.icons.Patreon
import org.junit.Test

class LazyVerticalGridTest : ComposeBaseTest() {

    private val items = arrayOf(
        GridItem(Icons.AutoMirrored.Rounded.HelpOutline, R.string.install_guide) {},
        GridItem(CustomIcons.Faq, R.string.faq_menu_item) {},
        GridItem(CustomIcons.Discord, R.string.about_discord_button_text) {},
        GridItem(Icons.Rounded.MailOutline, R.string.about_email_button_text) {},
        GridItem(CustomIcons.GitHub, R.string.about_github_button_text) {},
        GridItem(Icons.Rounded.Link, R.string.about_website_button_text) {},
        GridItem(CustomIcons.Patreon, R.string.about_patreon_button_text) {},
        GridItem(Icons.Rounded.StarOutline, R.string.about_rate_button_text) {},
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
