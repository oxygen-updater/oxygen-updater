package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Sync: ImageVector
    get() = _sync ?: materialSymbol(
        name = "Sync",
    ) {
        moveTo(240.0f, 482.0f)
        quadToRelative(0.0f, 45.0f, 17.0f, 87.5f)
        reflectiveQuadToRelative(53.0f, 78.5f)
        lineToRelative(10.0f, 10.0f)
        verticalLineToRelative(-58.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(360.0f, 560.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(400.0f, 600.0f)
        verticalLineToRelative(160.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(360.0f, 800.0f)
        lineTo(200.0f, 800.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(160.0f, 760.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(200.0f, 720.0f)
        horizontalLineToRelative(70.0f)
        lineToRelative(-16.0f, -14.0f)
        quadToRelative(-52.0f, -46.0f, -73.0f, -105.0f)
        reflectiveQuadToRelative(-21.0f, -119.0f)
        quadToRelative(0.0f, -94.0f, 48.0f, -170.5f)
        reflectiveQuadTo(337.0f, 194.0f)
        quadToRelative(14.0f, -8.0f, 29.5f, -1.0f)
        reflectiveQuadToRelative(20.5f, 23.0f)
        quadToRelative(5.0f, 15.0f, -0.5f, 30.0f)
        reflectiveQuadTo(367.0f, 269.0f)
        quadToRelative(-58.0f, 32.0f, -92.5f, 88.5f)
        reflectiveQuadTo(240.0f, 482.0f)
        close()
        moveTo(720.0f, 478.0f)
        quadToRelative(0.0f, -45.0f, -17.0f, -87.5f)
        reflectiveQuadTo(650.0f, 312.0f)
        lineToRelative(-10.0f, -10.0f)
        verticalLineToRelative(58.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(600.0f, 400.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(560.0f, 360.0f)
        verticalLineToRelative(-160.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(600.0f, 160.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(800.0f, 200.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(760.0f, 240.0f)
        horizontalLineToRelative(-70.0f)
        lineToRelative(16.0f, 14.0f)
        quadToRelative(49.0f, 49.0f, 71.5f, 106.5f)
        reflectiveQuadTo(800.0f, 478.0f)
        quadToRelative(0.0f, 94.0f, -48.0f, 170.5f)
        reflectiveQuadTo(623.0f, 766.0f)
        quadToRelative(-14.0f, 8.0f, -29.5f, 1.0f)
        reflectiveQuadTo(573.0f, 744.0f)
        quadToRelative(-5.0f, -15.0f, 0.5f, -30.0f)
        reflectiveQuadToRelative(19.5f, -23.0f)
        quadToRelative(58.0f, -32.0f, 92.5f, -88.5f)
        reflectiveQuadTo(720.0f, 478.0f)
        close()
    }.also { _sync = it }

private var _sync: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Sync)
