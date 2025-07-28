package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.KeyboardArrowUp: ImageVector
    get() = _keyboardArrowUp ?: materialSymbol(
        name = "KeyboardArrowUp",
    ) {
        moveTo(480f, 432f)
        lineTo(324f, 588f)
        quadToRelative(-11f, 11f, -28f, 11f)
        reflectiveQuadToRelative(-28f, -11f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        lineToRelative(184f, -184f)
        quadToRelative(12f, -12f, 28f, -12f)
        reflectiveQuadToRelative(28f, 12f)
        lineToRelative(184f, 184f)
        quadToRelative(11f, 11f, 11f, 28f)
        reflectiveQuadToRelative(-11f, 28f)
        quadToRelative(-11f, 11f, -28f, 11f)
        reflectiveQuadToRelative(-28f, -11f)
        lineTo(480f, 432f)
        close()
    }.also { _keyboardArrowUp = it }

private var _keyboardArrowUp: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.KeyboardArrowUp)
