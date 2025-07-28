package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.PlaylistAddCheck: ImageVector
    get() = _playlistAddCheck ?: materialSymbol(
        name = "PlaylistAddCheck",
        autoMirror = true,
    ) {
        moveTo(160f, 640f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 600f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 560f)
        horizontalLineToRelative(240f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(440f, 600f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(400f, 640f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 440f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 400f)
        horizontalLineToRelative(400f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(600f, 440f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(560f, 480f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 280f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 240f)
        horizontalLineToRelative(400f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(600f, 280f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(560f, 320f)
        horizontalLineTo(160f)
        close()
        moveToRelative(494f, 423f)
        quadToRelative(-8f, 0f, -15f, -2.5f)
        reflectiveQuadToRelative(-13f, -8.5f)
        lineToRelative(-86f, -86f)
        quadToRelative(-11f, -11f, -11.5f, -27.5f)
        reflectiveQuadTo(540f, 590f)
        quadToRelative(11f, -11f, 27.5f, -11.5f)
        reflectiveQuadTo(596f, 589f)
        lineToRelative(58f, 57f)
        lineToRelative(141f, -141f)
        quadToRelative(12f, -12f, 28.5f, -11.5f)
        reflectiveQuadTo(852f, 506f)
        quadToRelative(11f, 12f, 11.5f, 28f)
        reflectiveQuadTo(852f, 562f)
        lineTo(682f, 732f)
        quadToRelative(-6f, 6f, -13f, 8.5f)
        reflectiveQuadToRelative(-15f, 2.5f)
        close()
    }.also { _playlistAddCheck = it }

private var _playlistAddCheck: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.PlaylistAddCheck)

