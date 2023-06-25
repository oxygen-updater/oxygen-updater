package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes

@Composable
@NonRestartableComposable
fun GridItem(
    icon: ImageVector,
    @StringRes textResId: Int,
    onClick: () -> Unit,
) = IconText(
    Modifier
        .clickable(onClick = onClick)
        .borderExceptTop()
        .padding(16.dp),
    icon = icon, text = stringResource(textResId),
    style = MaterialTheme.typography.body2.copy(
        fontFamily = MaterialTheme.typography.subtitle1.fontFamily,
    )
)

@PreviewThemes
@Composable
fun PreviewGridItem() = PreviewAppTheme {
    GridItem(
        icon = Icons.Rounded.Android,
        textResId = R.string.app_name,
        onClick = {},
    )
}
