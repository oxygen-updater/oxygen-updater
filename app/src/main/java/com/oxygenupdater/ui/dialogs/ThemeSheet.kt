package com.oxygenupdater.ui.dialogs

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Check
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierSemanticsNotSelected
import com.oxygenupdater.ui.common.modifierSemanticsSelected
import com.oxygenupdater.ui.theme.LocalTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun ColumnScope.ThemeSheet(onClick: (Theme) -> Unit) {
    SheetHeader(R.string.label_theme)

    val selectedTheme = LocalTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Perf: re-use common modifiers to avoid recreating the same object repeatedly
    val iconSpacerSizeModifier = Modifier.size(40.dp) // 24 + 16
    LazyColumn(
        Modifier
            .weight(1f, false)
            .testTag(ThemeSheet_LazyColumnTestTag)
    ) {
        items(items = Themes, key = { it.value }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animatedClickable { onClick(it) }
                    .then(modifierDefaultPadding)
                    .testTag(BottomSheet_ItemRowTestTag)
            ) {
                val primary = colorScheme.primary
                val selected = selectedTheme == it
                if (selected) Icon(
                    imageVector = Symbols.Check,
                    contentDescription = stringResource(R.string.summary_on),
                    tint = primary,
                    modifier = Modifier.padding(end = 16.dp) then modifierSemanticsSelected
                ) else Spacer(iconSpacerSizeModifier then modifierSemanticsNotSelected)

                Column {
                    Text(
                        text = stringResource(it.titleResId),
                        color = if (selected) primary else Color.Unspecified,
                        style = typography.titleSmall,
                    )
                    Text(
                        text = stringResource(it.subtitleResId),
                        color = if (selected) primary else colorScheme.onSurfaceVariant,
                        style = typography.bodySmall,
                    )
                }
            }
        }
    }

    SheetCaption(R.string.theme_additional_note)
}

@VisibleForTesting
val Themes = arrayOf(
    Theme.Light,
    Theme.Dark,
    Theme.System,
    Theme.Auto,
)

private const val TAG = "ThemeSheet"

@VisibleForTesting
const val ThemeSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@PreviewThemes
@Composable
fun PreviewThemeSheet() = PreviewModalBottomSheet {
    ThemeSheet {}
}
