package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Mobile: ImageVector
    get() = _mobile ?: materialSymbol(
        name = "Mobile",
    ) {
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
        moveTo(480.0f, 240.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, -11.5f)
        reflectiveQuadTo(520.0f, 200.0f)
        quadToRelative(0.0f, -17.0f, -11.5f, -28.5f)
        reflectiveQuadTo(480.0f, 160.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, 11.5f)
        reflectiveQuadTo(440.0f, 200.0f)
        quadToRelative(0.0f, 17.0f, 11.5f, 28.5f)
        reflectiveQuadTo(480.0f, 240.0f)
        close()
    }.also { _mobile = it }

private var _mobile: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Mobile)
