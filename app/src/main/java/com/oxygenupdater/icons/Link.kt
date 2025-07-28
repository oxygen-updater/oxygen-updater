package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Link: ImageVector
    get() = _link ?: materialSymbol(
        name = "Link",
    ) {
        moveTo(280f, 680f)
        quadToRelative(-83f, 0f, -141.5f, -58.5f)
        reflectiveQuadTo(80f, 480f)
        quadToRelative(0f, -83f, 58.5f, -141.5f)
        reflectiveQuadTo(280f, 280f)
        horizontalLineToRelative(120f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(440f, 320f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(400f, 360f)
        horizontalLineTo(280f)
        quadToRelative(-50f, 0f, -85f, 35f)
        reflectiveQuadToRelative(-35f, 85f)
        quadToRelative(0f, 50f, 35f, 85f)
        reflectiveQuadToRelative(85f, 35f)
        horizontalLineToRelative(120f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(440f, 640f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(400f, 680f)
        horizontalLineTo(280f)
        close()
        moveToRelative(80f, -160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(320f, 480f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(360f, 440f)
        horizontalLineToRelative(240f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(640f, 480f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(600f, 520f)
        horizontalLineTo(360f)
        close()
        moveToRelative(200f, 160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(520f, 640f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(560f, 600f)
        horizontalLineToRelative(120f)
        quadToRelative(50f, 0f, 85f, -35f)
        reflectiveQuadToRelative(35f, -85f)
        quadToRelative(0f, -50f, -35f, -85f)
        reflectiveQuadToRelative(-85f, -35f)
        horizontalLineTo(560f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(520f, 320f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(560f, 280f)
        horizontalLineToRelative(120f)
        quadToRelative(83f, 0f, 141.5f, 58.5f)
        reflectiveQuadTo(880f, 480f)
        quadToRelative(0f, 83f, -58.5f, 141.5f)
        reflectiveQuadTo(680f, 680f)
        horizontalLineTo(560f)
        close()
    }.also { _link = it }

private var _link: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Link)
