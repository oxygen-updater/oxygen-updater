package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Notifications: ImageVector
    get() = _notifications ?: materialSymbol(
        name = "Notifications",
    ) {
        moveTo(200.0f, 760.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(160.0f, 720.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(200.0f, 680.0f)
        horizontalLineToRelative(40.0f)
        verticalLineToRelative(-280.0f)
        quadToRelative(0.0f, -83.0f, 50.0f, -147.5f)
        reflectiveQuadTo(420.0f, 168.0f)
        verticalLineToRelative(-28.0f)
        quadToRelative(0.0f, -25.0f, 17.5f, -42.5f)
        reflectiveQuadTo(480.0f, 80.0f)
        quadToRelative(25.0f, 0.0f, 42.5f, 17.5f)
        reflectiveQuadTo(540.0f, 140.0f)
        verticalLineToRelative(28.0f)
        quadToRelative(80.0f, 20.0f, 130.0f, 84.5f)
        reflectiveQuadTo(720.0f, 400.0f)
        verticalLineToRelative(280.0f)
        horizontalLineToRelative(40.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(800.0f, 720.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(760.0f, 760.0f)
        lineTo(200.0f, 760.0f)
        close()
        moveTo(480.0f, 460.0f)
        close()
        moveTo(480.0f, 880.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(400.0f, 800.0f)
        horizontalLineToRelative(160.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(480.0f, 880.0f)
        close()
        moveTo(320.0f, 680.0f)
        horizontalLineToRelative(320.0f)
        verticalLineToRelative(-280.0f)
        quadToRelative(0.0f, -66.0f, -47.0f, -113.0f)
        reflectiveQuadToRelative(-113.0f, -47.0f)
        quadToRelative(-66.0f, 0.0f, -113.0f, 47.0f)
        reflectiveQuadToRelative(-47.0f, 113.0f)
        verticalLineToRelative(280.0f)
        close()
    }.also { _notifications = it }

private var _notifications: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Notifications)
