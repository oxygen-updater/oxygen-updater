package com.oxygenupdater.ui.common

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Android
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

/**
 * Button with a leading [icon] followed by [Text].
 *
 * @param filled if true (default), style as a filled button (primary bg); otherwise,
 *   [OutlinedButton] with a primary border.
 */
@Composable
fun IconTextButton(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes textResId: Int,
    filled: Boolean = true,
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .requiredSize(ButtonDefaults.IconSize)
                .testTag(IconTextButton_IconTestTag)
        )
        Text(
            text = stringResource(textResId),
            modifier = Modifier
                .padding(start = ButtonDefaults.IconSpacing)
                .testTag(IconTextButton_TextTestTag)
        )
    }

    Button(
        onClick = onClick,
        colors = if (filled) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
        border = if (filled) null else BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = content,
        modifier = Modifier.testTag(IconTextButtonTestTag)
    )
}

private const val TAG = "IconTextButton"

@VisibleForTesting
const val IconTextButtonTestTag = TAG

@VisibleForTesting
const val IconTextButton_IconTestTag = TAG + "_Icon"

@VisibleForTesting
const val IconTextButton_TextTestTag = TAG + "_Text"

@PreviewThemes
@Composable
fun PreviewIconTextButton() = PreviewAppTheme {
    Column {
        arrayOf(true, false).forEach {
            IconTextButton(
                onClick = {},
                icon = Symbols.Android,
                textResId = R.string.app_name,
                filled = it,
            )
        }
    }
}
