package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Backspace: ImageVector
    get() = _backspace ?: materialSymbol(
        name = "Backspace",
        autoMirror = true,
    ) {
        moveToRelative(560.0f, 536.0f)
        lineToRelative(76.0f, 76.0f)
        quadToRelative(11.0f, 11.0f, 28.0f, 11.0f)
        reflectiveQuadToRelative(28.0f, -11.0f)
        quadToRelative(11.0f, -11.0f, 11.0f, -28.0f)
        reflectiveQuadToRelative(-11.0f, -28.0f)
        lineToRelative(-76.0f, -76.0f)
        lineToRelative(76.0f, -76.0f)
        quadToRelative(11.0f, -11.0f, 11.0f, -28.0f)
        reflectiveQuadToRelative(-11.0f, -28.0f)
        quadToRelative(-11.0f, -11.0f, -28.0f, -11.0f)
        reflectiveQuadToRelative(-28.0f, 11.0f)
        lineToRelative(-76.0f, 76.0f)
        lineToRelative(-76.0f, -76.0f)
        quadToRelative(-11.0f, -11.0f, -28.0f, -11.0f)
        reflectiveQuadToRelative(-28.0f, 11.0f)
        quadToRelative(-11.0f, 11.0f, -11.0f, 28.0f)
        reflectiveQuadToRelative(11.0f, 28.0f)
        lineToRelative(76.0f, 76.0f)
        lineToRelative(-76.0f, 76.0f)
        quadToRelative(-11.0f, 11.0f, -11.0f, 28.0f)
        reflectiveQuadToRelative(11.0f, 28.0f)
        quadToRelative(11.0f, 11.0f, 28.0f, 11.0f)
        reflectiveQuadToRelative(28.0f, -11.0f)
        lineToRelative(76.0f, -76.0f)
        close()
        moveTo(360.0f, 800.0f)
        quadToRelative(-19.0f, 0.0f, -36.0f, -8.5f)
        reflectiveQuadTo(296.0f, 768.0f)
        lineTo(116.0f, 528.0f)
        quadToRelative(-16.0f, -21.0f, -16.0f, -48.0f)
        reflectiveQuadToRelative(16.0f, -48.0f)
        lineToRelative(180.0f, -240.0f)
        quadToRelative(11.0f, -15.0f, 28.0f, -23.5f)
        reflectiveQuadToRelative(36.0f, -8.5f)
        horizontalLineToRelative(440.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, 23.5f)
        reflectiveQuadTo(880.0f, 240.0f)
        verticalLineToRelative(480.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(800.0f, 800.0f)
        lineTo(360.0f, 800.0f)
        close()
        moveTo(360.0f, 720.0f)
        horizontalLineToRelative(440.0f)
        verticalLineToRelative(-480.0f)
        lineTo(360.0f, 240.0f)
        lineTo(180.0f, 480.0f)
        lineToRelative(180.0f, 240.0f)
        close()
        moveTo(490.0f, 480.0f)
        close()
    }.also { _backspace = it }

private var _backspace: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Backspace)
