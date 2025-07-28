package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Star: ImageVector
    get() = _star ?: materialSymbol(
        name = "Star",
    ) {
        moveToRelative(354.0f, 673.0f)
        lineToRelative(126.0f, -76.0f)
        lineToRelative(126.0f, 77.0f)
        lineToRelative(-33.0f, -144.0f)
        lineToRelative(111.0f, -96.0f)
        lineToRelative(-146.0f, -13.0f)
        lineToRelative(-58.0f, -136.0f)
        lineToRelative(-58.0f, 135.0f)
        lineToRelative(-146.0f, 13.0f)
        lineToRelative(111.0f, 97.0f)
        lineToRelative(-33.0f, 143.0f)
        close()
        moveTo(480.0f, 691.0f)
        lineTo(314.0f, 791.0f)
        quadToRelative(-11.0f, 7.0f, -23.0f, 6.0f)
        reflectiveQuadToRelative(-21.0f, -8.0f)
        quadToRelative(-9.0f, -7.0f, -14.0f, -17.5f)
        reflectiveQuadToRelative(-2.0f, -23.5f)
        lineToRelative(44.0f, -189.0f)
        lineToRelative(-147.0f, -127.0f)
        quadToRelative(-10.0f, -9.0f, -12.5f, -20.5f)
        reflectiveQuadTo(140.0f, 389.0f)
        quadToRelative(4.0f, -11.0f, 12.0f, -18.0f)
        reflectiveQuadToRelative(22.0f, -9.0f)
        lineToRelative(194.0f, -17.0f)
        lineToRelative(75.0f, -178.0f)
        quadToRelative(5.0f, -12.0f, 15.5f, -18.0f)
        reflectiveQuadToRelative(21.5f, -6.0f)
        quadToRelative(11.0f, 0.0f, 21.5f, 6.0f)
        reflectiveQuadToRelative(15.5f, 18.0f)
        lineToRelative(75.0f, 178.0f)
        lineToRelative(194.0f, 17.0f)
        quadToRelative(14.0f, 2.0f, 22.0f, 9.0f)
        reflectiveQuadToRelative(12.0f, 18.0f)
        quadToRelative(4.0f, 11.0f, 1.5f, 22.5f)
        reflectiveQuadTo(809.0f, 432.0f)
        lineTo(662.0f, 559.0f)
        lineToRelative(44.0f, 189.0f)
        quadToRelative(3.0f, 13.0f, -2.0f, 23.5f)
        reflectiveQuadTo(690.0f, 789.0f)
        quadToRelative(-9.0f, 7.0f, -21.0f, 8.0f)
        reflectiveQuadToRelative(-23.0f, -6.0f)
        lineTo(480.0f, 691.0f)
        close()
        moveTo(480.0f, 490.0f)
        close()
    }.also { _star = it }

private var _star: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Star)
