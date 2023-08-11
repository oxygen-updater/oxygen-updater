package com.oxygenupdater.compose.ui.common

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oxygenupdater.compose.ui.theme.backgroundVariant

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun ItemDivider(modifier: Modifier = Modifier) = HorizontalDivider(
    modifier,
    color = MaterialTheme.colorScheme.backgroundVariant
)
