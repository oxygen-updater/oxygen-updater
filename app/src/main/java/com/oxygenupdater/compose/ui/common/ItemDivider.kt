package com.oxygenupdater.compose.ui.common

import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import com.oxygenupdater.compose.ui.theme.backgroundVariant

@Composable
fun ItemDivider(modifier: Modifier = Modifier) = Divider(
    modifier,
    color = MaterialTheme.colors.backgroundVariant,
)
