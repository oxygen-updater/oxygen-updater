package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun ColumnScope.DropdownMenuItem(
    icon: ImageVector,
    @StringRes textResId: Int,
    onClick: () -> Unit,
) = DropdownMenuItem(onClick) {
    Icon(icon, stringResource(R.string.icon), tint = MaterialTheme.colors.primary)
    Text(
        stringResource(textResId),
        Modifier
            .padding(start = 16.dp)
            .wrapContentSize(unbounded = true),
        style = MaterialTheme.typography.body2
    )
}
