package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Warning: ImageVector
    get() = _warning ?: materialSymbol(
        name = "Warning",
    ) {
        moveTo(109f, 840f)
        quadToRelative(-11f, 0f, -20f, -5.5f)
        reflectiveQuadTo(75f, 820f)
        quadToRelative(-5f, -9f, -5.5f, -19.5f)
        reflectiveQuadTo(75f, 780f)
        lineToRelative(370f, -640f)
        quadToRelative(6f, -10f, 15.5f, -15f)
        reflectiveQuadToRelative(19.5f, -5f)
        quadToRelative(10f, 0f, 19.5f, 5f)
        reflectiveQuadToRelative(15.5f, 15f)
        lineToRelative(370f, 640f)
        quadToRelative(6f, 10f, 5.5f, 20.5f)
        reflectiveQuadTo(885f, 820f)
        quadToRelative(-5f, 9f, -14f, 14.5f)
        reflectiveQuadToRelative(-20f, 5.5f)
        horizontalLineTo(109f)
        close()
        moveToRelative(69f, -80f)
        horizontalLineToRelative(604f)
        lineTo(480f, 240f)
        lineTo(178f, 760f)
        close()
        moveToRelative(302f, -40f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(520f, 680f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(480f, 640f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(440f, 680f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(480f, 720f)
        close()
        moveToRelative(0f, -120f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(520f, 560f)
        verticalLineToRelative(-120f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(480f, 400f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(440f, 440f)
        verticalLineToRelative(120f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(480f, 600f)
        close()
        moveToRelative(0f, -100f)
        close()
    }.also { _warning = it }

private var _warning: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Warning)

