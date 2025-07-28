package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Check: ImageVector
    get() = _check ?: materialSymbol(
        name = "Check",
    ) {
        moveToRelative(382.0f, 606.0f)
        lineToRelative(339.0f, -339.0f)
        quadToRelative(12.0f, -12.0f, 28.0f, -12.0f)
        reflectiveQuadToRelative(28.0f, 12.0f)
        quadToRelative(12.0f, 12.0f, 12.0f, 28.5f)
        reflectiveQuadTo(777.0f, 324.0f)
        lineTo(410.0f, 692.0f)
        quadToRelative(-12.0f, 12.0f, -28.0f, 12.0f)
        reflectiveQuadToRelative(-28.0f, -12.0f)
        lineTo(182.0f, 520.0f)
        quadToRelative(-12.0f, -12.0f, -11.5f, -28.5f)
        reflectiveQuadTo(183.0f, 463.0f)
        quadToRelative(12.0f, -12.0f, 28.5f, -12.0f)
        reflectiveQuadToRelative(28.5f, 12.0f)
        lineToRelative(142.0f, 143.0f)
        close()
    }.also { _check = it }

private var _check: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Check)
