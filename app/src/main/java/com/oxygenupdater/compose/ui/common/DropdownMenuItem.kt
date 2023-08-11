package com.oxygenupdater.compose.ui.common

import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R

@Composable
fun DropdownMenuItem(
    icon: ImageVector,
    @StringRes textResId: Int,
    onClick: () -> Unit,
) = DropdownMenuItem({
    Text(stringResource(textResId), style = MaterialTheme.typography.bodyMedium)
}, onClick, leadingIcon = {
    Icon(icon, stringResource(R.string.icon), tint = MaterialTheme.colorScheme.primary)
})
