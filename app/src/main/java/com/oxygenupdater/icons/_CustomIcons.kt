package com.oxygenupdater.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.oxygenupdater.ui.theme.PreviewAppTheme

object CustomIcons

@Composable
fun PreviewIcon(icon: ImageVector) = PreviewAppTheme {
    Icon(icon, null)
}
