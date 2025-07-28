package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.OpenInBrowser: ImageVector
    get() = _openInBrowser ?: materialSymbol(
        name = "OpenInBrowser",
    ) {
        moveTo(200f, 840f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 760f)
        verticalLineToRelative(-560f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 120f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 200f)
        verticalLineToRelative(560f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 840f)
        horizontalLineTo(640f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(600f, 800f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(640f, 760f)
        horizontalLineToRelative(120f)
        verticalLineToRelative(-480f)
        horizontalLineTo(200f)
        verticalLineToRelative(480f)
        horizontalLineToRelative(120f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(360f, 800f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(320f, 840f)
        horizontalLineTo(200f)
        close()
        moveToRelative(240f, -40f)
        verticalLineToRelative(-206f)
        lineToRelative(-35f, 35f)
        quadToRelative(-12f, 12f, -28.5f, 11.5f)
        reflectiveQuadTo(348f, 628f)
        quadToRelative(-11f, -12f, -11.5f, -28f)
        reflectiveQuadToRelative(11.5f, -28f)
        lineToRelative(104f, -104f)
        quadToRelative(6f, -6f, 13f, -8.5f)
        reflectiveQuadToRelative(15f, -2.5f)
        quadToRelative(8f, 0f, 15f, 2.5f)
        reflectiveQuadToRelative(13f, 8.5f)
        lineToRelative(104f, 104f)
        quadToRelative(12f, 12f, 11.5f, 28f)
        reflectiveQuadTo(612f, 628f)
        quadToRelative(-12f, 12f, -28.5f, 12.5f)
        reflectiveQuadTo(555f, 629f)
        lineToRelative(-35f, -35f)
        verticalLineToRelative(206f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(480f, 840f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(440f, 800f)
        close()
    }.also { _openInBrowser = it }

private var _openInBrowser: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.OpenInBrowser)
