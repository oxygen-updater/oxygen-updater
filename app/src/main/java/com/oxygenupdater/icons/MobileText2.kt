package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.MobileText2: ImageVector
    get() = _mobileText2 ?: materialSymbol(
        name = "MobileText2",
    ) {
        moveTo(360.0f, 480.0f)
        horizontalLineToRelative(240.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, -11.5f)
        reflectiveQuadTo(640.0f, 440.0f)
        quadToRelative(0.0f, -17.0f, -11.5f, -28.5f)
        reflectiveQuadTo(600.0f, 400.0f)
        lineTo(360.0f, 400.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, 11.5f)
        reflectiveQuadTo(320.0f, 440.0f)
        quadToRelative(0.0f, 17.0f, 11.5f, 28.5f)
        reflectiveQuadTo(360.0f, 480.0f)
        close()
        moveTo(400.0f, 620.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, -11.5f)
        reflectiveQuadTo(600.0f, 580.0f)
        quadToRelative(0.0f, -17.0f, -11.5f, -28.5f)
        reflectiveQuadTo(560.0f, 540.0f)
        lineTo(400.0f, 540.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, 11.5f)
        reflectiveQuadTo(360.0f, 580.0f)
        quadToRelative(0.0f, 17.0f, 11.5f, 28.5f)
        reflectiveQuadTo(400.0f, 620.0f)
        close()
        moveTo(280.0f, 920.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(200.0f, 840.0f)
        verticalLineToRelative(-720.0f)
        quadToRelative(0.0f, -33.0f, 23.5f, -56.5f)
        reflectiveQuadTo(280.0f, 40.0f)
        horizontalLineToRelative(400.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, 23.5f)
        reflectiveQuadTo(760.0f, 120.0f)
        verticalLineToRelative(124.0f)
        quadToRelative(18.0f, 7.0f, 29.0f, 22.0f)
        reflectiveQuadToRelative(11.0f, 34.0f)
        verticalLineToRelative(80.0f)
        quadToRelative(0.0f, 19.0f, -11.0f, 34.0f)
        reflectiveQuadToRelative(-29.0f, 22.0f)
        verticalLineToRelative(404.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(680.0f, 920.0f)
        lineTo(280.0f, 920.0f)
        close()
        moveTo(280.0f, 840.0f)
        horizontalLineToRelative(400.0f)
        verticalLineToRelative(-720.0f)
        lineTo(280.0f, 120.0f)
        verticalLineToRelative(720.0f)
        close()
        moveTo(280.0f, 840.0f)
        verticalLineToRelative(-720.0f)
        verticalLineToRelative(720.0f)
        close()
    }.also { _mobileText2 = it }

private var _mobileText2: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.MobileText2)
