package com.oxygenupdater.compose.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.oxygenupdater.compose.ui.theme.AppTheme

fun Fragment.setContent(
    content: @Composable () -> Unit,
) = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        AppTheme {
            Surface(content = content)
        }
    }
}
