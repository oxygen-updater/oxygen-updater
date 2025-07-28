package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.DoneAll: ImageVector
    get() = _doneAll ?: materialSymbol(
        name = "DoneAll",
    ) {
        moveTo(70.0f, 522.0f)
        quadToRelative(-12.0f, -12.0f, -11.5f, -28.0f)
        reflectiveQuadTo(71.0f, 466.0f)
        quadToRelative(12.0f, -11.0f, 28.0f, -11.5f)
        reflectiveQuadToRelative(28.0f, 11.5f)
        lineToRelative(142.0f, 142.0f)
        lineToRelative(14.0f, 14.0f)
        lineToRelative(14.0f, 14.0f)
        quadToRelative(12.0f, 12.0f, 11.5f, 28.0f)
        reflectiveQuadTo(296.0f, 692.0f)
        quadToRelative(-12.0f, 11.0f, -28.0f, 11.5f)
        reflectiveQuadTo(240.0f, 692.0f)
        lineTo(70.0f, 522.0f)
        close()
        moveTo(494.0f, 607.0f)
        lineTo(834.0f, 267.0f)
        quadToRelative(12.0f, -12.0f, 28.0f, -11.5f)
        reflectiveQuadToRelative(28.0f, 12.5f)
        quadToRelative(11.0f, 12.0f, 11.5f, 28.0f)
        reflectiveQuadTo(890.0f, 324.0f)
        lineTo(522.0f, 692.0f)
        quadToRelative(-12.0f, 12.0f, -28.0f, 12.0f)
        reflectiveQuadToRelative(-28.0f, -12.0f)
        lineTo(296.0f, 522.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -27.5f)
        reflectiveQuadToRelative(11.0f, -28.5f)
        quadToRelative(12.0f, -12.0f, 28.5f, -12.0f)
        reflectiveQuadToRelative(28.5f, 12.0f)
        lineToRelative(141.0f, 141.0f)
        close()
        moveTo(663.0f, 325.0f)
        lineTo(522.0f, 466.0f)
        quadToRelative(-11.0f, 11.0f, -27.5f, 11.0f)
        reflectiveQuadTo(466.0f, 466.0f)
        quadToRelative(-12.0f, -12.0f, -12.0f, -28.5f)
        reflectiveQuadToRelative(12.0f, -28.5f)
        lineToRelative(141.0f, -141.0f)
        quadToRelative(11.0f, -11.0f, 27.5f, -11.0f)
        reflectiveQuadToRelative(28.5f, 11.0f)
        quadToRelative(12.0f, 12.0f, 12.0f, 28.5f)
        reflectiveQuadTo(663.0f, 325.0f)
        close()
    }.also { _doneAll = it }

private var _doneAll: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.DoneAll)
