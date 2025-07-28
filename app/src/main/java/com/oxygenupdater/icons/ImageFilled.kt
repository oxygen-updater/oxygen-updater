package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.ImageFilled: ImageVector
    get() = _imageFilled ?: materialSymbol(
        name = "ImageFilled",
    ) {
        moveTo(200.0f, 840.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(120.0f, 760.0f)
        verticalLineToRelative(-560.0f)
        quadToRelative(0.0f, -33.0f, 23.5f, -56.5f)
        reflectiveQuadTo(200.0f, 120.0f)
        horizontalLineToRelative(560.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, 23.5f)
        reflectiveQuadTo(840.0f, 200.0f)
        verticalLineToRelative(560.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(760.0f, 840.0f)
        lineTo(200.0f, 840.0f)
        close()
        moveTo(280.0f, 680.0f)
        horizontalLineToRelative(400.0f)
        quadToRelative(12.0f, 0.0f, 18.0f, -11.0f)
        reflectiveQuadToRelative(-2.0f, -21.0f)
        lineTo(586.0f, 501.0f)
        quadToRelative(-6.0f, -8.0f, -16.0f, -8.0f)
        reflectiveQuadToRelative(-16.0f, 8.0f)
        lineTo(450.0f, 640.0f)
        lineToRelative(-74.0f, -99.0f)
        quadToRelative(-6.0f, -8.0f, -16.0f, -8.0f)
        reflectiveQuadToRelative(-16.0f, 8.0f)
        lineToRelative(-80.0f, 107.0f)
        quadToRelative(-8.0f, 10.0f, -2.0f, 21.0f)
        reflectiveQuadToRelative(18.0f, 11.0f)
        close()
    }.also { _imageFilled = it }

private var _imageFilled: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.ImageFilled)
