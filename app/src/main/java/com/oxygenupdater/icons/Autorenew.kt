package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Autorenew: ImageVector
    get() = _autorenew ?: materialSymbol(
        name = "Autorenew",
    ) {
        moveTo(240.0f, 482.0f)
        quadToRelative(0.0f, 16.0f, 2.0f, 31.5f)
        reflectiveQuadToRelative(7.0f, 30.5f)
        quadToRelative(5.0f, 17.0f, -1.0f, 32.5f)
        reflectiveQuadTo(227.0f, 599.0f)
        quadToRelative(-16.0f, 8.0f, -31.5f, 1.5f)
        reflectiveQuadTo(175.0f, 577.0f)
        quadToRelative(-8.0f, -23.0f, -11.5f, -47.0f)
        reflectiveQuadToRelative(-3.5f, -48.0f)
        quadToRelative(0.0f, -134.0f, 93.0f, -228.0f)
        reflectiveQuadToRelative(227.0f, -94.0f)
        horizontalLineToRelative(7.0f)
        lineToRelative(-36.0f, -36.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        lineToRelative(104.0f, 104.0f)
        quadToRelative(12.0f, 12.0f, 12.0f, 28.0f)
        reflectiveQuadToRelative(-12.0f, 28.0f)
        lineTo(507.0f, 332.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        lineToRelative(36.0f, -36.0f)
        horizontalLineToRelative(-7.0f)
        quadToRelative(-100.0f, 0.0f, -170.0f, 70.5f)
        reflectiveQuadTo(240.0f, 482.0f)
        close()
        moveTo(720.0f, 478.0f)
        quadToRelative(0.0f, -16.0f, -2.0f, -31.5f)
        reflectiveQuadToRelative(-7.0f, -30.5f)
        quadToRelative(-5.0f, -17.0f, 1.0f, -32.5f)
        reflectiveQuadToRelative(21.0f, -22.5f)
        quadToRelative(16.0f, -8.0f, 31.5f, -1.5f)
        reflectiveQuadTo(785.0f, 383.0f)
        quadToRelative(8.0f, 23.0f, 11.5f, 47.0f)
        reflectiveQuadToRelative(3.5f, 48.0f)
        quadToRelative(0.0f, 134.0f, -93.0f, 228.0f)
        reflectiveQuadToRelative(-227.0f, 94.0f)
        horizontalLineToRelative(-7.0f)
        lineToRelative(36.0f, 36.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        lineTo(349.0f, 788.0f)
        quadToRelative(-12.0f, -12.0f, -12.0f, -28.0f)
        reflectiveQuadToRelative(12.0f, -28.0f)
        lineToRelative(104.0f, -104.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        lineToRelative(-36.0f, 36.0f)
        horizontalLineToRelative(7.0f)
        quadToRelative(100.0f, 0.0f, 170.0f, -70.5f)
        reflectiveQuadTo(720.0f, 478.0f)
        close()
    }.also { _autorenew = it }

private var _autorenew: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Autorenew)
