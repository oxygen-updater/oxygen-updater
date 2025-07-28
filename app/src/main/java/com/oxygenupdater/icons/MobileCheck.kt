package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.MobileCheck: ImageVector
    get() = _mobileCheck ?: materialSymbol(
        name = "MobileCheck",
    ) {
        moveTo(280f, 920f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(200f, 840f)
        verticalLineToRelative(-720f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(280f, 40f)
        horizontalLineToRelative(400f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(760f, 120f)
        verticalLineToRelative(124f)
        quadToRelative(18f, 7f, 29f, 22f)
        reflectiveQuadToRelative(11f, 34f)
        verticalLineToRelative(80f)
        quadToRelative(0f, 19f, -11f, 34f)
        reflectiveQuadToRelative(-29f, 22f)
        verticalLineToRelative(404f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(680f, 920f)
        horizontalLineTo(280f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(400f)
        verticalLineToRelative(-720f)
        horizontalLineTo(280f)
        verticalLineToRelative(720f)
        close()
        moveToRelative(0f, 0f)
        verticalLineToRelative(-720f)
        verticalLineToRelative(720f)
        close()
        moveToRelative(162f, -352f)
        lineToRelative(-28f, -28f)
        quadToRelative(-11f, -11f, -28f, -11f)
        reflectiveQuadToRelative(-28f, 11f)
        quadToRelative(-11f, 11f, -11f, 28f)
        reflectiveQuadToRelative(11f, 28f)
        lineToRelative(56f, 56f)
        quadToRelative(12f, 12f, 28f, 12f)
        reflectiveQuadToRelative(28f, -12f)
        lineToRelative(142f, -142f)
        quadToRelative(11f, -11f, 11f, -28f)
        reflectiveQuadToRelative(-11f, -28f)
        quadToRelative(-11f, -11f, -28f, -11f)
        reflectiveQuadToRelative(-28f, 11f)
        lineTo(442f, 488f)
        close()
    }.also { _mobileCheck = it }

private var _mobileCheck: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.MobileCheck)
