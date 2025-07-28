package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Refresh: ImageVector
    get() = _refresh ?: materialSymbol(
        name = "Refresh",
    ) {
        moveTo(480.0f, 800.0f)
        quadToRelative(-134.0f, 0.0f, -227.0f, -93.0f)
        reflectiveQuadToRelative(-93.0f, -227.0f)
        quadToRelative(0.0f, -134.0f, 93.0f, -227.0f)
        reflectiveQuadToRelative(227.0f, -93.0f)
        quadToRelative(69.0f, 0.0f, 132.0f, 28.5f)
        reflectiveQuadTo(720.0f, 270.0f)
        verticalLineToRelative(-70.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(760.0f, 160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(800.0f, 200.0f)
        verticalLineToRelative(200.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(760.0f, 440.0f)
        lineTo(560.0f, 440.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(520.0f, 400.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(560.0f, 360.0f)
        horizontalLineToRelative(128.0f)
        quadToRelative(-32.0f, -56.0f, -87.5f, -88.0f)
        reflectiveQuadTo(480.0f, 240.0f)
        quadToRelative(-100.0f, 0.0f, -170.0f, 70.0f)
        reflectiveQuadToRelative(-70.0f, 170.0f)
        quadToRelative(0.0f, 100.0f, 70.0f, 170.0f)
        reflectiveQuadToRelative(170.0f, 70.0f)
        quadToRelative(68.0f, 0.0f, 124.5f, -34.5f)
        reflectiveQuadTo(692.0f, 593.0f)
        quadToRelative(8.0f, -14.0f, 22.5f, -19.5f)
        reflectiveQuadToRelative(29.5f, -0.5f)
        quadToRelative(16.0f, 5.0f, 23.0f, 21.0f)
        reflectiveQuadToRelative(-1.0f, 30.0f)
        quadToRelative(-41.0f, 80.0f, -117.0f, 128.0f)
        reflectiveQuadToRelative(-169.0f, 48.0f)
        close()
    }.also { _refresh = it }

private var _refresh: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Refresh)
