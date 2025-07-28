package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Shield: ImageVector
    get() = _shield ?: materialSymbol(
        name = "Shield",
    ) {
        moveTo(480.0f, 876.0f)
        quadToRelative(-7.0f, 0.0f, -13.0f, -1.0f)
        reflectiveQuadToRelative(-12.0f, -3.0f)
        quadToRelative(-135.0f, -45.0f, -215.0f, -166.5f)
        reflectiveQuadTo(160.0f, 444.0f)
        verticalLineToRelative(-189.0f)
        quadToRelative(0.0f, -25.0f, 14.5f, -45.0f)
        reflectiveQuadToRelative(37.5f, -29.0f)
        lineToRelative(240.0f, -90.0f)
        quadToRelative(14.0f, -5.0f, 28.0f, -5.0f)
        reflectiveQuadToRelative(28.0f, 5.0f)
        lineToRelative(240.0f, 90.0f)
        quadToRelative(23.0f, 9.0f, 37.5f, 29.0f)
        reflectiveQuadToRelative(14.5f, 45.0f)
        verticalLineToRelative(189.0f)
        quadToRelative(0.0f, 140.0f, -80.0f, 261.5f)
        reflectiveQuadTo(505.0f, 872.0f)
        quadToRelative(-6.0f, 2.0f, -12.0f, 3.0f)
        reflectiveQuadToRelative(-13.0f, 1.0f)
        close()
        moveTo(480.0f, 796.0f)
        quadToRelative(104.0f, -33.0f, 172.0f, -132.0f)
        reflectiveQuadToRelative(68.0f, -220.0f)
        verticalLineToRelative(-189.0f)
        lineToRelative(-240.0f, -90.0f)
        lineToRelative(-240.0f, 90.0f)
        verticalLineToRelative(189.0f)
        quadToRelative(0.0f, 121.0f, 68.0f, 220.0f)
        reflectiveQuadToRelative(172.0f, 132.0f)
        close()
        moveTo(480.0f, 480.0f)
        close()
    }.also { _shield = it }

private var _shield: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Shield)
