package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.SystemUpdateAlt: ImageVector
    get() = _systemUpdateAlt ?: materialSymbol(
        name = "SystemUpdateAlt",
    ) {
        moveTo(160.0f, 800.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(80.0f, 720.0f)
        verticalLineToRelative(-480.0f)
        quadToRelative(0.0f, -33.0f, 23.5f, -56.5f)
        reflectiveQuadTo(160.0f, 160.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(360.0f, 200.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(320.0f, 240.0f)
        lineTo(160.0f, 240.0f)
        verticalLineToRelative(480.0f)
        horizontalLineToRelative(640.0f)
        verticalLineToRelative(-480.0f)
        lineTo(640.0f, 240.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(600.0f, 200.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(640.0f, 160.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, 23.5f)
        reflectiveQuadTo(880.0f, 240.0f)
        verticalLineToRelative(480.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(800.0f, 800.0f)
        lineTo(160.0f, 800.0f)
        close()
        moveTo(440.0f, 464.0f)
        verticalLineToRelative(-264.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(480.0f, 160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(520.0f, 200.0f)
        verticalLineToRelative(264.0f)
        lineToRelative(76.0f, -76.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        lineTo(508.0f, 588.0f)
        quadToRelative(-12.0f, 12.0f, -28.0f, 12.0f)
        reflectiveQuadToRelative(-28.0f, -12.0f)
        lineTo(308.0f, 444.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        quadToRelative(11.0f, -11.0f, 28.0f, -11.0f)
        reflectiveQuadToRelative(28.0f, 11.0f)
        lineToRelative(76.0f, 76.0f)
        close()
    }.also { _systemUpdateAlt = it }

private var _systemUpdateAlt: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.SystemUpdateAlt)
