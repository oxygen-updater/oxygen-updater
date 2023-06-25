package com.oxygenupdater.compose.ui.common

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** Vertically centered [Checkbox] with [Text]. Tapping text toggles checkbox state. */
@Composable
@SuppressLint("ModifierParameter")
fun CheckboxText(
    checked: MutableState<Boolean> = remember { mutableStateOf(true) },
    @StringRes textResId: Int,
    modifier: Modifier = Modifier, textModifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    val interactionSource = remember { MutableInteractionSource() }
    Checkbox(checked.value, { checked.value = it }, interactionSource = interactionSource)

    Text(
        stringResource(textResId),
        textModifier
            .weight(1f)
            .clickable(interactionSource, null) {
                checked.value = !checked.value
            },
        style = MaterialTheme.typography.caption
    )

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}
