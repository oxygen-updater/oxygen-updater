package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes

/** Leading [icon] with [Text] in an [OutlinedButton] with a tined border */
@Composable
@NonRestartableComposable
fun OutlinedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes textResId: Int,
) = OutlinedButton(
    onClick,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    Icon(icon, null, Modifier.requiredSize(18.dp))
    Text(stringResource(textResId), Modifier.padding(start = 4.dp))
}

@PreviewThemes
@Composable
fun PreviewOutlinedIconButton() = PreviewAppTheme {
    OutlinedIconButton(
        onClick = {},
        icon = Icons.Rounded.Android,
        textResId = R.string.app_name,
    )
}
