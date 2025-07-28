package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Cancel: ImageVector
    get() = _cancel ?: materialSymbol(
        name = "Cancel",
    ) {
        moveToRelative(480.0f, 536.0f)
        lineToRelative(116.0f, 116.0f)
        quadToRelative(11.0f, 11.0f, 28.0f, 11.0f)
        reflectiveQuadToRelative(28.0f, -11.0f)
        quadToRelative(11.0f, -11.0f, 11.0f, -28.0f)
        reflectiveQuadToRelative(-11.0f, -28.0f)
        lineTo(536.0f, 480.0f)
        lineToRelative(116.0f, -116.0f)
        quadToRelative(11.0f, -11.0f, 11.0f, -28.0f)
        reflectiveQuadToRelative(-11.0f, -28.0f)
        quadToRelative(-11.0f, -11.0f, -28.0f, -11.0f)
        reflectiveQuadToRelative(-28.0f, 11.0f)
        lineTo(480.0f, 424.0f)
        lineTo(364.0f, 308.0f)
        quadToRelative(-11.0f, -11.0f, -28.0f, -11.0f)
        reflectiveQuadToRelative(-28.0f, 11.0f)
        quadToRelative(-11.0f, 11.0f, -11.0f, 28.0f)
        reflectiveQuadToRelative(11.0f, 28.0f)
        lineToRelative(116.0f, 116.0f)
        lineToRelative(-116.0f, 116.0f)
        quadToRelative(-11.0f, 11.0f, -11.0f, 28.0f)
        reflectiveQuadToRelative(11.0f, 28.0f)
        quadToRelative(11.0f, 11.0f, 28.0f, 11.0f)
        reflectiveQuadToRelative(28.0f, -11.0f)
        lineToRelative(116.0f, -116.0f)
        close()
        moveTo(480.0f, 880.0f)
        quadToRelative(-83.0f, 0.0f, -156.0f, -31.5f)
        reflectiveQuadTo(197.0f, 763.0f)
        quadToRelative(-54.0f, -54.0f, -85.5f, -127.0f)
        reflectiveQuadTo(80.0f, 480.0f)
        quadToRelative(0.0f, -83.0f, 31.5f, -156.0f)
        reflectiveQuadTo(197.0f, 197.0f)
        quadToRelative(54.0f, -54.0f, 127.0f, -85.5f)
        reflectiveQuadTo(480.0f, 80.0f)
        quadToRelative(83.0f, 0.0f, 156.0f, 31.5f)
        reflectiveQuadTo(763.0f, 197.0f)
        quadToRelative(54.0f, 54.0f, 85.5f, 127.0f)
        reflectiveQuadTo(880.0f, 480.0f)
        quadToRelative(0.0f, 83.0f, -31.5f, 156.0f)
        reflectiveQuadTo(763.0f, 763.0f)
        quadToRelative(-54.0f, 54.0f, -127.0f, 85.5f)
        reflectiveQuadTo(480.0f, 880.0f)
        close()
        moveTo(480.0f, 800.0f)
        quadToRelative(134.0f, 0.0f, 227.0f, -93.0f)
        reflectiveQuadToRelative(93.0f, -227.0f)
        quadToRelative(0.0f, -134.0f, -93.0f, -227.0f)
        reflectiveQuadToRelative(-227.0f, -93.0f)
        quadToRelative(-134.0f, 0.0f, -227.0f, 93.0f)
        reflectiveQuadToRelative(-93.0f, 227.0f)
        quadToRelative(0.0f, 134.0f, 93.0f, 227.0f)
        reflectiveQuadToRelative(227.0f, 93.0f)
        close()
        moveTo(480.0f, 480.0f)
        close()
    }.also { _cancel = it }

private var _cancel: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Cancel)
