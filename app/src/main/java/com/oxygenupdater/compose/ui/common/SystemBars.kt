package com.oxygenupdater.compose.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.oxygenupdater.compose.ui.theme.light

@Composable
fun TransparentSystemBars() {
    val controller = rememberSystemUiController()
    controller.setSystemBarsColor(Color.Transparent, MaterialTheme.colorScheme.light)
}
