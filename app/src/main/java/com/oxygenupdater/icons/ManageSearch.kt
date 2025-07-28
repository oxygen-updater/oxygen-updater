package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.ManageSearch: ImageVector
    get() = _manageSearch ?: materialSymbol(
        name = "ManageSearch",
    ) {
        moveTo(120.0f, 760.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(80.0f, 720.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(120.0f, 680.0f)
        horizontalLineToRelative(320.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(480.0f, 720.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(440.0f, 760.0f)
        lineTo(120.0f, 760.0f)
        close()
        moveTo(120.0f, 560.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(80.0f, 520.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(120.0f, 480.0f)
        horizontalLineToRelative(120.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(280.0f, 520.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(240.0f, 560.0f)
        lineTo(120.0f, 560.0f)
        close()
        moveTo(120.0f, 360.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(80.0f, 320.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(120.0f, 280.0f)
        horizontalLineToRelative(120.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(280.0f, 320.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(240.0f, 360.0f)
        lineTo(120.0f, 360.0f)
        close()
        moveTo(560.0f, 640.0f)
        quadToRelative(-83.0f, 0.0f, -141.5f, -58.5f)
        reflectiveQuadTo(360.0f, 440.0f)
        quadToRelative(0.0f, -83.0f, 58.5f, -141.5f)
        reflectiveQuadTo(560.0f, 240.0f)
        quadToRelative(83.0f, 0.0f, 141.5f, 58.5f)
        reflectiveQuadTo(760.0f, 440.0f)
        quadToRelative(0.0f, 29.0f, -8.5f, 57.5f)
        reflectiveQuadTo(726.0f, 550.0f)
        lineToRelative(126.0f, 126.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        lineTo(670.0f, 606.0f)
        quadToRelative(-24.0f, 17.0f, -52.5f, 25.5f)
        reflectiveQuadTo(560.0f, 640.0f)
        close()
        moveTo(560.0f, 560.0f)
        quadToRelative(50.0f, 0.0f, 85.0f, -35.0f)
        reflectiveQuadToRelative(35.0f, -85.0f)
        quadToRelative(0.0f, -50.0f, -35.0f, -85.0f)
        reflectiveQuadToRelative(-85.0f, -35.0f)
        quadToRelative(-50.0f, 0.0f, -85.0f, 35.0f)
        reflectiveQuadToRelative(-35.0f, 85.0f)
        quadToRelative(0.0f, 50.0f, 35.0f, 85.0f)
        reflectiveQuadToRelative(85.0f, 35.0f)
        close()
    }.also { _manageSearch = it }

private var _manageSearch: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.ManageSearch)
