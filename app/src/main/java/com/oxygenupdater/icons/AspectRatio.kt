package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.AspectRatio: ImageVector
    get() = _aspectRatio ?: materialSymbol(
        name = "AspectRatio",
    ) {
        moveTo(560f, 680f)
        horizontalLineToRelative(200f)
        verticalLineToRelative(-200f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(120f)
        horizontalLineTo(560f)
        verticalLineToRelative(80f)
        close()
        moveTo(200f, 480f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-120f)
        horizontalLineToRelative(120f)
        verticalLineToRelative(-80f)
        horizontalLineTo(200f)
        verticalLineToRelative(200f)
        close()
        moveToRelative(-40f, 320f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(80f, 720f)
        verticalLineToRelative(-480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 160f)
        horizontalLineToRelative(640f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(880f, 240f)
        verticalLineToRelative(480f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(800f, 800f)
        horizontalLineTo(160f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(640f)
        verticalLineToRelative(-480f)
        horizontalLineTo(160f)
        verticalLineToRelative(480f)
        close()
        moveToRelative(0f, 0f)
        verticalLineToRelative(-480f)
        verticalLineToRelative(480f)
        close()
    }.also { _aspectRatio = it }

private var _aspectRatio: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.AspectRatio)
