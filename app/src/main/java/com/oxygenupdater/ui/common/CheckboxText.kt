package com.oxygenupdater.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

/** Vertically centered [Checkbox] with [Text]. Tapping text toggles checkbox state. */
@Composable
fun CheckboxText(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    val interactionSource = remember { MutableInteractionSource() }
    Checkbox(checked, onCheckedChange, interactionSource = interactionSource)

    Text(
        text = stringResource(textResId),
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        modifier = textModifier.clickable(interactionSource, null) { onCheckedChange(!checked) }
    )
}
