package com.oxygenupdater.compose.ui.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.News
import com.oxygenupdater.compose.icons.Settings
import com.oxygenupdater.compose.ui.common.IconText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes

@Composable
fun HelpScreen() = Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = 16.dp), // must be after `verticalScroll`
) {
    // Updating your device
    HelpItem(
        Icons.Rounded.SystemUpdateAlt,
        R.string.help_updating_your_device_title,
        R.string.help_updating_your_device_text,
    )

    // Reading news
    HelpItem(
        CustomIcons.News,
        R.string.help_reading_news_title,
        R.string.help_reading_news_text,
    )

    // Viewing device information
    HelpItem(
        Icons.Rounded.PhoneAndroid,
        R.string.help_device_information_title,
        R.string.help_device_information_text,
    )

    // Changing settings
    HelpItem(
        CustomIcons.Settings,
        R.string.help_change_settings_title,
        R.string.help_change_settings_text,
        true // last item
    )
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    @StringRes titleResId: Int,
    @StringRes textResId: Int,
    last: Boolean = false,
) {
    IconText(
        Modifier.padding(horizontal = 16.dp),
        icon = icon, text = stringResource(titleResId),
        style = MaterialTheme.typography.subtitle1
    )

    SelectionContainer(if (last) Modifier.navigationBarsPadding() else Modifier) {
        Text(
            stringResource(textResId),
            Modifier.padding(start = 56.dp, end = 16.dp),
            style = MaterialTheme.typography.body2
        )
    }

    if (!last) ItemDivider(Modifier.padding(vertical = 16.dp))
}

@PreviewThemes
@Composable
fun PreviewHelpScreen() = PreviewAppTheme {
    HelpScreen()
}
