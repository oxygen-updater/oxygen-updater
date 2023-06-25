package com.oxygenupdater.compose.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
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
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes

@Composable
@NonRestartableComposable
fun IconText(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    icon: ImageVector, text: String,
    iconTint: Color = MaterialTheme.colors.primary,
    style: TextStyle = MaterialTheme.typography.body2,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, stringResource(R.string.icon), tint = iconTint)

    // Apply padding first
    Text(text, Modifier.padding(start = 16.dp) then textModifier, style = style)

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

@PreviewThemes
@Composable
fun PreviewIconText() = PreviewAppTheme {
    IconText(
        Modifier.padding(16.dp),
        icon = Icons.Rounded.Android,
        text = stringResource(R.string.app_name),
    )
}
