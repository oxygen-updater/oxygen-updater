package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.LockOpen: ImageVector
    get() = _lockOpen ?: materialSymbol(
        name = "LockOpen",
    ) {
        moveTo(240f, 880f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(160f, 800f)
        verticalLineToRelative(-400f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 320f)
        horizontalLineToRelative(360f)
        verticalLineToRelative(-80f)
        quadToRelative(0f, -50f, -35f, -85f)
        reflectiveQuadToRelative(-85f, -35f)
        quadToRelative(-42f, 0f, -73.5f, 25.5f)
        reflectiveQuadTo(364f, 209f)
        quadToRelative(-4f, 14f, -16.5f, 22.5f)
        reflectiveQuadTo(320f, 240f)
        quadToRelative(-17f, 0f, -28.5f, -11f)
        reflectiveQuadToRelative(-8.5f, -26f)
        quadToRelative(14f, -69f, 69f, -116f)
        reflectiveQuadToRelative(128f, -47f)
        quadToRelative(83f, 0f, 141.5f, 58.5f)
        reflectiveQuadTo(680f, 240f)
        verticalLineToRelative(80f)
        horizontalLineToRelative(40f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 400f)
        verticalLineToRelative(400f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(720f, 880f)
        horizontalLineTo(240f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(480f)
        verticalLineToRelative(-400f)
        horizontalLineTo(240f)
        verticalLineToRelative(400f)
        close()
        moveToRelative(240f, -120f)
        quadToRelative(33f, 0f, 56.5f, -23.5f)
        reflectiveQuadTo(560f, 600f)
        quadToRelative(0f, -33f, -23.5f, -56.5f)
        reflectiveQuadTo(480f, 520f)
        quadToRelative(-33f, 0f, -56.5f, 23.5f)
        reflectiveQuadTo(400f, 600f)
        quadToRelative(0f, 33f, 23.5f, 56.5f)
        reflectiveQuadTo(480f, 680f)
        close()
        moveTo(240f, 800f)
        verticalLineToRelative(-400f)
        verticalLineToRelative(400f)
        close()
    }.also { _lockOpen = it }

private var _lockOpen: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.LockOpen)
