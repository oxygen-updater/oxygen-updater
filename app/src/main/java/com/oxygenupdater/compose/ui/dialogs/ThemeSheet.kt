package com.oxygenupdater.compose.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.Theme
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.internal.settings.PrefManager

private val list = arrayOf(
    Theme.Light,
    Theme.Dark,
    Theme.System,
    Theme.Auto,
)

@Composable
fun ColumnScope.ThemeSheet(
    hide: () -> Unit,
    onClick: (Theme) -> Unit,
) {
    SheetHeader(R.string.label_theme, hide)

    val selectedTheme = remember { PrefManager.theme }

    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography
    LazyColumn(Modifier.weight(1f, false)) {
        items(list, { it.value }) {
            Row(
                Modifier
                    .animatedClickable {
                        PrefManager.putInt(PrefManager.PROPERTY_THEME_ID, it.value)
                        onClick(it)
                        hide()
                    }
                    .padding(16.dp), // must be after `clickable`
                verticalAlignment = Alignment.CenterVertically
            ) {
                val positive = colors.positive
                val selected = selectedTheme == it
                if (selected) Icon(
                    Icons.Rounded.Done, stringResource(R.string.summary_on),
                    Modifier.padding(end = 16.dp),
                    tint = positive,
                ) else Spacer(Modifier.size(40.dp)) // 24 + 16

                Column {
                    val color = if (selected) positive else Color.Unspecified
                    Text(stringResource(it.titleResId), color = color, style = typography.subtitle2)
                    Text(
                        stringResource(it.subtitleResId),
                        Modifier.alpha(ContentAlpha.high),
                        color, style = typography.caption
                    )
                }
            }
        }
    }

    SheetCaption(R.string.theme_additional_note)
}

@PreviewThemes
@Composable
fun PreviewThemeSheet() = PreviewModalBottomSheet {
    ThemeSheet(hide = {}) {}
}
