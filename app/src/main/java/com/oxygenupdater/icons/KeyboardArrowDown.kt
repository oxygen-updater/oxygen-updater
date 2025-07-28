package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.KeyboardArrowDown: ImageVector
    get() = _keyboardArrowDown ?: materialSymbol(
        name = "KeyboardArrowDown",
    ) {
        moveTo(480f, 599f)
        quadToRelative(-8f, 0f, -15f, -2.5f)
        reflectiveQuadToRelative(-13f, -8.5f)
        lineTo(268f, 404f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        lineToRelative(156f, 156f)
        lineToRelative(156f, -156f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        quadToRelative(11f, 11f, 11f, 28f)
        reflectiveQuadToRelative(-11f, 28f)
        lineTo(508f, 588f)
        quadToRelative(-6f, 6f, -13f, 8.5f)
        reflectiveQuadToRelative(-15f, 2.5f)
        close()
    }.also { _keyboardArrowDown = it }

private var _keyboardArrowDown: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.KeyboardArrowDown)
