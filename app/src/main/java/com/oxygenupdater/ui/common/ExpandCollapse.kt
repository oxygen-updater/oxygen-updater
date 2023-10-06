package com.oxygenupdater.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

@Composable
@NonRestartableComposable
fun ExpandCollapse(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) = AnimatedVisibility(
    visible = visible,
    enter = remember {
        expandVertically(spring(visibilityThreshold = IntSize.VisibilityThreshold)) + fadeIn(initialAlpha = .3f)
    },
    exit = remember {
        shrinkVertically(spring(visibilityThreshold = IntSize.VisibilityThreshold)) + fadeOut()
    },
    label = "ExpandCollapseAnimatedVisibility",
    content = content,
    modifier = modifier
)
