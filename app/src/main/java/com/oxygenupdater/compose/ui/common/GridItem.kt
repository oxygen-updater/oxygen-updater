package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.backgroundVariant

@Composable
fun GridItem(
    icon: ImageVector,
    @StringRes textResId: Int,
    onClick: () -> Unit,
) = IconText(
    Modifier
        .clickable(onClick = onClick)
        .borderExceptTop(MaterialTheme.colorScheme.backgroundVariant)
        .padding(16.dp),
    icon = icon, text = stringResource(textResId),
    style = MaterialTheme.typography.titleSmall
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
