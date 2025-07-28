package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.ArrowBack: ImageVector
    get() = _arrowBack ?: materialSymbol(
        name = "ArrowBack",
        autoMirror = true,
    ) {
        moveToRelative(313.0f, 520.0f)
        lineToRelative(196.0f, 196.0f)
        quadToRelative(12.0f, 12.0f, 11.5f, 28.0f)
        reflectiveQuadTo(508.0f, 772.0f)
        quadToRelative(-12.0f, 11.0f, -28.0f, 11.5f)
        reflectiveQuadTo(452.0f, 772.0f)
        lineTo(188.0f, 508.0f)
        quadToRelative(-6.0f, -6.0f, -8.5f, -13.0f)
        reflectiveQuadToRelative(-2.5f, -15.0f)
        quadToRelative(0.0f, -8.0f, 2.5f, -15.0f)
        reflectiveQuadToRelative(8.5f, -13.0f)
        lineToRelative(264.0f, -264.0f)
        quadToRelative(11.0f, -11.0f, 27.5f, -11.0f)
        reflectiveQuadToRelative(28.5f, 11.0f)
        quadToRelative(12.0f, 12.0f, 12.0f, 28.5f)
        reflectiveQuadTo(508.0f, 245.0f)
        lineTo(313.0f, 440.0f)
        horizontalLineToRelative(447.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(800.0f, 480.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(760.0f, 520.0f)
        lineTo(313.0f, 520.0f)
        close()
    }.also { _arrowBack = it }

private var _arrowBack: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.ArrowBack)
