package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.OpenInNew: ImageVector
    get() = _openInNew ?: materialSymbol(
        name = "OpenInNew",
        autoMirror = true,
    ) {
        moveTo(200.0f, 840.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(120.0f, 760.0f)
        verticalLineToRelative(-560.0f)
        quadToRelative(0.0f, -33.0f, 23.5f, -56.5f)
        reflectiveQuadTo(200.0f, 120.0f)
        horizontalLineToRelative(240.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(480.0f, 160.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(440.0f, 200.0f)
        lineTo(200.0f, 200.0f)
        verticalLineToRelative(560.0f)
        horizontalLineToRelative(560.0f)
        verticalLineToRelative(-240.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(800.0f, 480.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(840.0f, 520.0f)
        verticalLineToRelative(240.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(760.0f, 840.0f)
        lineTo(200.0f, 840.0f)
        close()
        moveTo(760.0f, 256.0f)
        lineTo(416.0f, 600.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        quadToRelative(-11.0f, -11.0f, -11.0f, -28.0f)
        reflectiveQuadToRelative(11.0f, -28.0f)
        lineToRelative(344.0f, -344.0f)
        lineTo(600.0f, 200.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(560.0f, 160.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(600.0f, 120.0f)
        horizontalLineToRelative(200.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(840.0f, 160.0f)
        verticalLineToRelative(200.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(800.0f, 400.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(760.0f, 360.0f)
        verticalLineToRelative(-104.0f)
        close()
    }.also { _openInNew = it }

private var _openInNew: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.OpenInNew)
