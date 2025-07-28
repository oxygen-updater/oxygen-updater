package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Close: ImageVector
    get() = _close ?: materialSymbol(
        name = "Close",
    ) {
        moveTo(480.0f, 536.0f)
        lineTo(284.0f, 732.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        lineToRelative(196.0f, -196.0f)
        lineToRelative(-196.0f, -196.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        lineToRelative(196.0f, 196.0f)
        lineToRelative(196.0f, -196.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        lineTo(536.0f, 480.0f)
        lineToRelative(196.0f, 196.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        lineTo(480.0f, 536.0f)
        close()
    }.also { _close = it }

private var _close: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Close)
