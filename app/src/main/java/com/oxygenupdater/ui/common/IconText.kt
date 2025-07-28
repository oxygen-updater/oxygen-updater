package com.oxygenupdater.ui.common

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Android
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
@NonRestartableComposable
fun IconText(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    icon: ImageVector, text: String,

    /** Useful if [textModifier] has [androidx.compose.foundation.basicMarquee] applied */
    maxLines: Int = Int.MAX_VALUE,

    iconTint: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.testTag(IconTextTestTag)
) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = iconTint,
        modifier = Modifier.requiredSize(24.dp)
    )

    Text(
        text = text,
        maxLines = maxLines,
        style = style,
        // Apply padding first
        modifier = modifierDefaultPaddingStart then textModifier
    )

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

private const val TAG = "IconText"

@VisibleForTesting
const val IconTextTestTag = TAG

@PreviewThemes
@Composable
fun PreviewIconText() = PreviewAppTheme {
    IconText(
        icon = Symbols.Android,
        text = stringResource(R.string.app_name),
        modifier = modifierDefaultPadding
    )
}
