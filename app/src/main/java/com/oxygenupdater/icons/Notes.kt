package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Notes: ImageVector
    get() = _notes ?: materialSymbol(
        name = "Notes",
        autoMirror = true,
    ) {
        moveTo(160f, 720f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 680f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 640f)
        horizontalLineToRelative(400f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(600f, 680f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(560f, 720f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -200f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 480f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 440f)
        horizontalLineToRelative(640f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(840f, 480f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(800f, 520f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -200f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 280f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 240f)
        horizontalLineToRelative(640f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(840f, 280f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(800f, 320f)
        horizontalLineTo(160f)
        close()
    }.also { _notes = it }

private var _notes: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Notes)

