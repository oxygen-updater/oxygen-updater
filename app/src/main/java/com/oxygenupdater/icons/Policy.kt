package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Policy: ImageVector
    get() = _policy ?: materialSymbol(
        name = "Policy",
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
        quadToRelative(0.0f, 75.0f, -23.5f, 146.5f)
        reflectiveQuadTo(709.0f, 720.0f)
        quadToRelative(-8.0f, 11.0f, -21.5f, 11.5f)
        reflectiveQuadTo(664.0f, 722.0f)
        lineTo(560.0f, 618.0f)
        quadToRelative(-18.0f, 11.0f, -38.5f, 16.5f)
        reflectiveQuadTo(480.0f, 640.0f)
        quadToRelative(-66.0f, 0.0f, -113.0f, -47.0f)
        reflectiveQuadToRelative(-47.0f, -113.0f)
        quadToRelative(0.0f, -66.0f, 47.0f, -113.0f)
        reflectiveQuadToRelative(113.0f, -47.0f)
        quadToRelative(66.0f, 0.0f, 113.0f, 47.0f)
        reflectiveQuadToRelative(47.0f, 113.0f)
        quadToRelative(0.0f, 22.0f, -5.5f, 42.5f)
        reflectiveQuadTo(618.0f, 562.0f)
        lineToRelative(60.0f, 60.0f)
        quadToRelative(20.0f, -41.0f, 31.0f, -86.0f)
        reflectiveQuadToRelative(11.0f, -92.0f)
        verticalLineToRelative(-189.0f)
        lineToRelative(-240.0f, -90.0f)
        lineToRelative(-240.0f, 90.0f)
        verticalLineToRelative(189.0f)
        quadToRelative(0.0f, 121.0f, 68.0f, 220.0f)
        reflectiveQuadToRelative(172.0f, 132.0f)
        quadToRelative(16.0f, -5.0f, 31.5f, -12.0f)
        reflectiveQuadToRelative(30.5f, -16.0f)
        quadToRelative(14.0f, -8.0f, 30.5f, -6.0f)
        reflectiveQuadToRelative(26.5f, 16.0f)
        quadToRelative(10.0f, 14.0f, 6.5f, 30.0f)
        reflectiveQuadTo(588.0f, 833.0f)
        quadToRelative(-20.0f, 12.0f, -40.0f, 21.5f)
        reflectiveQuadTo(505.0f, 872.0f)
        quadToRelative(-6.0f, 2.0f, -12.0f, 3.0f)
        reflectiveQuadToRelative(-13.0f, 1.0f)
        close()
        moveTo(480.0f, 560.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, -23.5f)
        reflectiveQuadTo(560.0f, 480.0f)
        quadToRelative(0.0f, -33.0f, -23.5f, -56.5f)
        reflectiveQuadTo(480.0f, 400.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, 23.5f)
        reflectiveQuadTo(400.0f, 480.0f)
        quadToRelative(0.0f, 33.0f, 23.5f, 56.5f)
        reflectiveQuadTo(480.0f, 560.0f)
        close()
        moveTo(488.0f, 483.0f)
        close()
    }.also { _policy = it }

private var _policy: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Policy)
