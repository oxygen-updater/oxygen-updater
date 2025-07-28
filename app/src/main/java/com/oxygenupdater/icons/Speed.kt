package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Speed: ImageVector
    get() = _speed ?: materialSymbol(
        name = "Speed",
    ) {
        moveTo(418f, 620f)
        quadToRelative(24f, 24f, 62f, 23.5f)
        reflectiveQuadToRelative(56f, -27.5f)
        lineToRelative(169f, -253f)
        quadToRelative(9f, -14f, -2.5f, -25.5f)
        reflectiveQuadTo(677f, 335f)
        lineTo(424f, 504f)
        quadToRelative(-27f, 18f, -28.5f, 55f)
        reflectiveQuadToRelative(22.5f, 61f)
        close()
        moveToRelative(62f, -460f)
        quadToRelative(36f, 0f, 71f, 6f)
        reflectiveQuadToRelative(68f, 19f)
        quadToRelative(16f, 6f, 34f, 22.5f)
        reflectiveQuadToRelative(10f, 31.5f)
        quadToRelative(-8f, 15f, -36f, 20f)
        reflectiveQuadToRelative(-45f, -1f)
        quadToRelative(-25f, -9f, -50.5f, -13.5f)
        reflectiveQuadTo(480f, 240f)
        quadToRelative(-133f, 0f, -226.5f, 93.5f)
        reflectiveQuadTo(160f, 560f)
        quadToRelative(0f, 42f, 11.5f, 83f)
        reflectiveQuadToRelative(32.5f, 77f)
        horizontalLineToRelative(552f)
        quadToRelative(23f, -38f, 33.5f, -79f)
        reflectiveQuadToRelative(10.5f, -85f)
        quadToRelative(0f, -26f, -4.5f, -51f)
        reflectiveQuadTo(782f, 456f)
        quadToRelative(-6f, -17f, -2f, -33f)
        reflectiveQuadToRelative(18f, -27f)
        quadToRelative(13f, -10f, 28.5f, -6f)
        reflectiveQuadToRelative(21.5f, 18f)
        quadToRelative(15f, 35f, 23f, 71.5f)
        reflectiveQuadToRelative(9f, 74.5f)
        quadToRelative(1f, 57f, -13f, 109f)
        reflectiveQuadToRelative(-41f, 99f)
        quadToRelative(-11f, 18f, -30f, 28f)
        reflectiveQuadToRelative(-40f, 10f)
        horizontalLineTo(204f)
        quadToRelative(-21f, 0f, -40f, -10f)
        reflectiveQuadToRelative(-30f, -28f)
        quadToRelative(-26f, -45f, -40f, -95.5f)
        reflectiveQuadTo(80f, 560f)
        quadToRelative(0f, -83f, 31.5f, -155.5f)
        reflectiveQuadToRelative(86f, -127f)
        quadTo(252f, 223f, 325f, 191.5f)
        reflectiveQuadTo(480f, 160f)
        close()
        moveToRelative(7f, 313f)
        close()
    }.also { _speed = it }

private var _speed: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Speed)
