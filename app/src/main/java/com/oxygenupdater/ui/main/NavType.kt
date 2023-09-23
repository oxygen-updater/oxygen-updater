package com.oxygenupdater.ui.main

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@JvmInline
value class NavType(val value: Int) {

    override fun toString() = "NavType." + when (this) {
        BottomBar -> "BottomBar"
        SideRail -> "SideRail"
        else -> "Invalid"
    }

    companion object {
        @Stable
        val BottomBar = NavType(0)

        @Stable
        val SideRail = NavType(1)

        fun from(windowWidthSize: WindowWidthSizeClass) = when (windowWidthSize) {
            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> SideRail
            else -> BottomBar
        }
    }
}
