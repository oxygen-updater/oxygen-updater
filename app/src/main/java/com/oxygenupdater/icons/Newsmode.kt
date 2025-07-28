package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Newsmode: ImageVector
    get() = _newsmode ?: materialSymbol(
        name = "Newsmode",
        autoMirror = true,
    ) {
        moveTo(160f, 840f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(80f, 760f)
        verticalLineToRelative(-560f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 120f)
        horizontalLineToRelative(640f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(880f, 200f)
        verticalLineToRelative(560f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(800f, 840f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(640f)
        verticalLineToRelative(-560f)
        horizontalLineTo(160f)
        verticalLineToRelative(560f)
        close()
        moveToRelative(120f, -80f)
        horizontalLineToRelative(400f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(720f, 640f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(680f, 600f)
        horizontalLineTo(280f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(240f, 640f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(280f, 680f)
        close()
        moveToRelative(0f, -160f)
        horizontalLineToRelative(80f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(400f, 480f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(360f, 280f)
        horizontalLineToRelative(-80f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(240f, 320f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(280f, 520f)
        close()
        moveToRelative(240f, 0f)
        horizontalLineToRelative(160f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(720f, 480f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(680f, 440f)
        horizontalLineTo(520f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(480f, 480f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(520f, 520f)
        close()
        moveToRelative(0f, -160f)
        horizontalLineToRelative(160f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(720f, 320f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(680f, 280f)
        horizontalLineTo(520f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(480f, 320f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(520f, 360f)
        close()
        moveTo(160f, 760f)
        verticalLineToRelative(-560f)
        verticalLineToRelative(560f)
        close()
    }.also { _newsmode = it }

private var _newsmode: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Newsmode)

