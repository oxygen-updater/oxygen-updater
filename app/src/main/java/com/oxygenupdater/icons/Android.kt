package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Android: ImageVector
    get() = _android ?: materialSymbol(
        name = "Android",
    ) {
        moveTo(40.0f, 720.0f)
        quadToRelative(9.0f, -107.0f, 65.5f, -197.0f)
        reflectiveQuadTo(256.0f, 380.0f)
        lineToRelative(-74.0f, -128.0f)
        quadToRelative(-6.0f, -9.0f, -3.0f, -19.0f)
        reflectiveQuadToRelative(13.0f, -15.0f)
        quadToRelative(8.0f, -5.0f, 18.0f, -2.0f)
        reflectiveQuadToRelative(16.0f, 12.0f)
        lineToRelative(74.0f, 128.0f)
        quadToRelative(86.0f, -36.0f, 180.0f, -36.0f)
        reflectiveQuadToRelative(180.0f, 36.0f)
        lineToRelative(74.0f, -128.0f)
        quadToRelative(6.0f, -9.0f, 16.0f, -12.0f)
        reflectiveQuadToRelative(18.0f, 2.0f)
        quadToRelative(10.0f, 5.0f, 13.0f, 15.0f)
        reflectiveQuadToRelative(-3.0f, 19.0f)
        lineToRelative(-74.0f, 128.0f)
        quadToRelative(94.0f, 53.0f, 150.5f, 143.0f)
        reflectiveQuadTo(920.0f, 720.0f)
        lineTo(40.0f, 720.0f)
        close()
        moveTo(280.0f, 610.0f)
        quadToRelative(21.0f, 0.0f, 35.5f, -14.5f)
        reflectiveQuadTo(330.0f, 560.0f)
        quadToRelative(0.0f, -21.0f, -14.5f, -35.5f)
        reflectiveQuadTo(280.0f, 510.0f)
        quadToRelative(-21.0f, 0.0f, -35.5f, 14.5f)
        reflectiveQuadTo(230.0f, 560.0f)
        quadToRelative(0.0f, 21.0f, 14.5f, 35.5f)
        reflectiveQuadTo(280.0f, 610.0f)
        close()
        moveTo(680.0f, 610.0f)
        quadToRelative(21.0f, 0.0f, 35.5f, -14.5f)
        reflectiveQuadTo(730.0f, 560.0f)
        quadToRelative(0.0f, -21.0f, -14.5f, -35.5f)
        reflectiveQuadTo(680.0f, 510.0f)
        quadToRelative(-21.0f, 0.0f, -35.5f, 14.5f)
        reflectiveQuadTo(630.0f, 560.0f)
        quadToRelative(0.0f, 21.0f, 14.5f, 35.5f)
        reflectiveQuadTo(680.0f, 610.0f)
        close()
    }.also { _android = it }

private var _android: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Android)
