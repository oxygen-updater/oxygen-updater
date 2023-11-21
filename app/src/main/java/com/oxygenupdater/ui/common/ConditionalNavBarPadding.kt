package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oxygenupdater.ui.main.NavType

/**
 * If [navType] is not [NavType.BottomBar], content must apply [Modifier.navigationBarsPadding].
 * However, if we do this in the topmost parent composable, we lose edge-to-edge immersiveness.
 *
 * Meant to be used as the last (vertical) composable in children of [com.oxygenupdater.activities.MainActivity]'s NavHost.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun ConditionalNavBarPadding(navType: NavType) {
    if (navType != NavType.BottomBar) Spacer(Modifier.navigationBarsPadding())
}
