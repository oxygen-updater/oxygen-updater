package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Download: ImageVector
    get() = _download ?: materialSymbol(
        name = "Download",
    ) {
        moveTo(480.0f, 623.0f)
        quadToRelative(-8.0f, 0.0f, -15.0f, -2.5f)
        reflectiveQuadToRelative(-13.0f, -8.5f)
        lineTo(308.0f, 468.0f)
        quadToRelative(-12.0f, -12.0f, -11.5f, -28.0f)
        reflectiveQuadToRelative(11.5f, -28.0f)
        quadToRelative(12.0f, -12.0f, 28.5f, -12.5f)
        reflectiveQuadTo(365.0f, 411.0f)
        lineToRelative(75.0f, 75.0f)
        verticalLineToRelative(-286.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(480.0f, 160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(520.0f, 200.0f)
        verticalLineToRelative(286.0f)
        lineToRelative(75.0f, -75.0f)
        quadToRelative(12.0f, -12.0f, 28.5f, -11.5f)
        reflectiveQuadTo(652.0f, 412.0f)
        quadToRelative(11.0f, 12.0f, 11.5f, 28.0f)
        reflectiveQuadTo(652.0f, 468.0f)
        lineTo(508.0f, 612.0f)
        quadToRelative(-6.0f, 6.0f, -13.0f, 8.5f)
        reflectiveQuadToRelative(-15.0f, 2.5f)
        close()
        moveTo(240.0f, 800.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(160.0f, 720.0f)
        verticalLineToRelative(-80.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(200.0f, 600.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(240.0f, 640.0f)
        verticalLineToRelative(80.0f)
        horizontalLineToRelative(480.0f)
        verticalLineToRelative(-80.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(760.0f, 600.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(800.0f, 640.0f)
        verticalLineToRelative(80.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(720.0f, 800.0f)
        lineTo(240.0f, 800.0f)
        close()
    }.also { _download = it }

private var _download: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Download)
