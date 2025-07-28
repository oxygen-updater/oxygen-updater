package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Forum: ImageVector
    get() = _forum ?: materialSymbol(
        name = "Forum",
    ) {
        moveTo(840f, 824f)
        quadToRelative(-8f, 0f, -15f, -3f)
        reflectiveQuadToRelative(-13f, -9f)
        lineToRelative(-92f, -92f)
        horizontalLineTo(320f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(240f, 640f)
        verticalLineToRelative(-40f)
        horizontalLineToRelative(440f)
        quadToRelative(33f, 0f, 56.5f, -23.5f)
        reflectiveQuadTo(760f, 520f)
        verticalLineToRelative(-280f)
        horizontalLineToRelative(40f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(880f, 320f)
        verticalLineToRelative(463f)
        quadToRelative(0f, 18f, -12f, 29.5f)
        reflectiveQuadTo(840f, 824f)
        close()
        moveTo(160f, 487f)
        lineToRelative(47f, -47f)
        horizontalLineToRelative(393f)
        verticalLineToRelative(-280f)
        horizontalLineTo(160f)
        verticalLineToRelative(327f)
        close()
        moveToRelative(-40f, 137f)
        quadToRelative(-16f, 0f, -28f, -11.5f)
        reflectiveQuadTo(80f, 583f)
        verticalLineToRelative(-423f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 80f)
        horizontalLineToRelative(440f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(680f, 160f)
        verticalLineToRelative(280f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(600f, 520f)
        horizontalLineTo(240f)
        lineToRelative(-92f, 92f)
        quadToRelative(-6f, 6f, -13f, 9f)
        reflectiveQuadToRelative(-15f, 3f)
        close()
        moveToRelative(40f, -184f)
        verticalLineToRelative(-280f)
        verticalLineToRelative(280f)
        close()
    }.also { _forum = it }

private var _forum: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Forum)

