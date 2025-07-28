package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.ShieldLock: ImageVector
    get() = _shieldLock ?: materialSymbol(
        name = "ShieldLock",
    ) {
        moveTo(400.0f, 640.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, -11.5f)
        reflectiveQuadTo(600.0f, 600.0f)
        verticalLineToRelative(-120.0f)
        quadToRelative(0.0f, -17.0f, -11.5f, -28.5f)
        reflectiveQuadTo(560.0f, 440.0f)
        verticalLineToRelative(-40.0f)
        quadToRelative(0.0f, -33.0f, -23.5f, -56.5f)
        reflectiveQuadTo(480.0f, 320.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, 23.5f)
        reflectiveQuadTo(400.0f, 400.0f)
        verticalLineToRelative(40.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, 11.5f)
        reflectiveQuadTo(360.0f, 480.0f)
        verticalLineToRelative(120.0f)
        quadToRelative(0.0f, 17.0f, 11.5f, 28.5f)
        reflectiveQuadTo(400.0f, 640.0f)
        close()
        moveTo(440.0f, 440.0f)
        verticalLineToRelative(-40.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(480.0f, 360.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(520.0f, 400.0f)
        verticalLineToRelative(40.0f)
        horizontalLineToRelative(-80.0f)
        close()
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
    }.also { _shieldLock = it }

private var _shieldLock: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.ShieldLock)
