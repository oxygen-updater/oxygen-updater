package com.oxygenupdater.ui.common

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

/** Leading [icon] with [Text] in an [OutlinedButton] with a tinted border */
@Composable
@NonRestartableComposable
fun OutlinedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes textResId: Int,
) = OutlinedButton(
    onClick = onClick,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier = Modifier.testTag(OutlinedIconButtonTestTag)
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .requiredSize(18.dp)
            .testTag(OutlinedIconButton_IconTestTag)
    )
    Text(
        text = stringResource(textResId),
        modifier = Modifier
            .padding(start = 4.dp)
            .testTag(OutlinedIconButton_TextTestTag)
    )
}

private const val TAG = "OutlinedIconButton"

@VisibleForTesting
const val OutlinedIconButtonTestTag = TAG

@VisibleForTesting
const val OutlinedIconButton_IconTestTag = TAG + "_Icon"

@VisibleForTesting
const val OutlinedIconButton_TextTestTag = TAG + "_Text"

@PreviewThemes
@Composable
fun PreviewOutlinedIconButton() = PreviewAppTheme {
    OutlinedIconButton(
        onClick = {},
        icon = Icons.Rounded.Android,
        textResId = R.string.app_name,
    )
}
