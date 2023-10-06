package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
@NonRestartableComposable
fun IconText(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    icon: ImageVector, text: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, stringResource(R.string.icon), Modifier.requiredSize(24.dp), tint = iconTint)

    Text(
        text = text,
        style = style,
        // Apply padding first
        modifier = modifierDefaultPaddingStart then textModifier
    )

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

@PreviewThemes
@Composable
fun PreviewIconText() = PreviewAppTheme {
    IconText(
        icon = Icons.Rounded.Android,
        text = stringResource(R.string.app_name),
        modifier = modifierDefaultPadding
    )
}
