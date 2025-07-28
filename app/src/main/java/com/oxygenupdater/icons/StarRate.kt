package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.StarRate: ImageVector
    get() = _starRate ?: materialSymbol(
        name = "StarRate",
    ) {
        moveToRelative(384.0f, 626.0f)
        lineToRelative(96.0f, -74.0f)
        lineToRelative(96.0f, 74.0f)
        lineToRelative(-36.0f, -122.0f)
        lineToRelative(90.0f, -64.0f)
        lineTo(518.0f, 440.0f)
        lineToRelative(-38.0f, -124.0f)
        lineToRelative(-38.0f, 124.0f)
        lineTo(330.0f, 440.0f)
        lineToRelative(90.0f, 64.0f)
        lineToRelative(-36.0f, 122.0f)
        close()
        moveTo(480.0f, 652.0f)
        lineTo(332.0f, 765.0f)
        quadToRelative(-11.0f, 9.0f, -24.0f, 8.5f)
        reflectiveQuadToRelative(-23.0f, -7.5f)
        quadToRelative(-10.0f, -7.0f, -15.5f, -19.0f)
        reflectiveQuadToRelative(-0.5f, -26.0f)
        lineToRelative(57.0f, -185.0f)
        lineToRelative(-145.0f, -103.0f)
        quadToRelative(-12.0f, -8.0f, -15.0f, -21.0f)
        reflectiveQuadToRelative(1.0f, -24.0f)
        quadToRelative(4.0f, -11.0f, 14.0f, -19.5f)
        reflectiveQuadToRelative(24.0f, -8.5f)
        horizontalLineToRelative(179.0f)
        lineToRelative(58.0f, -192.0f)
        quadToRelative(5.0f, -14.0f, 15.5f, -21.5f)
        reflectiveQuadTo(480.0f, 139.0f)
        quadToRelative(12.0f, 0.0f, 22.5f, 7.5f)
        reflectiveQuadTo(518.0f, 168.0f)
        lineToRelative(58.0f, 192.0f)
        horizontalLineToRelative(179.0f)
        quadToRelative(14.0f, 0.0f, 24.0f, 8.5f)
        reflectiveQuadToRelative(14.0f, 19.5f)
        quadToRelative(4.0f, 11.0f, 1.0f, 24.0f)
        reflectiveQuadToRelative(-15.0f, 21.0f)
        lineTo(634.0f, 536.0f)
        lineToRelative(57.0f, 185.0f)
        quadToRelative(5.0f, 14.0f, -0.5f, 26.0f)
        reflectiveQuadTo(675.0f, 766.0f)
        quadToRelative(-10.0f, 7.0f, -23.0f, 7.5f)
        reflectiveQuadToRelative(-24.0f, -8.5f)
        lineTo(480.0f, 652.0f)
        close()
        moveTo(480.0f, 471.0f)
        close()
    }.also { _starRate = it }

private var _starRate: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.StarRate)
