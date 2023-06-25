package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes

/**
 * Applies [CircleShape] to [OutlinedButton] and also tints border
 *
 * @param icon optional, displayed before [textResId]
 * @param contentColor applied to border and button contents (defaults to primary)
 */
@Composable
@NonRestartableComposable
fun OutlinedIconButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    @StringRes textResId: Int,
    contentColor: Color = MaterialTheme.colors.primary,
) = OutlinedButton(
    onClick,
    shape = CircleShape,
    border = BorderStroke(
        ButtonDefaults.OutlinedBorderSize,
        contentColor.copy(alpha = ButtonDefaults.OutlinedBorderOpacity)
    ),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
) {
    if (icon != null) Icon(
        icon, null,
        Modifier
            .requiredSize(18.dp)
            .padding(end = 4.dp)
    )
    Text(stringResource(textResId), color = contentColor)
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
