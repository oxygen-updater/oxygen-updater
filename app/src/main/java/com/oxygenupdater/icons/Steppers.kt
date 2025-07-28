package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Steppers: ImageVector
    get() = _steppers ?: materialSymbol(
        name = "steppers",
    ) {
        moveTo(200f, 600f)
        quadToRelative(-50f, 0f, -85f, -35f)
        reflectiveQuadToRelative(-35f, -85f)
        quadToRelative(0f, -50f, 35f, -85f)
        reflectiveQuadToRelative(85f, -35f)
        quadToRelative(50f, 0f, 85f, 35f)
        reflectiveQuadToRelative(35f, 85f)
        quadToRelative(0f, 50f, -35f, 85f)
        reflectiveQuadToRelative(-85f, 35f)
        close()
        moveToRelative(0f, -80f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(240f, 480f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(200f, 440f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(160f, 480f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(200f, 520f)
        close()
        moveToRelative(280f, 80f)
        quadToRelative(-50f, 0f, -85f, -35f)
        reflectiveQuadToRelative(-35f, -85f)
        quadToRelative(0f, -50f, 35f, -85f)
        reflectiveQuadToRelative(85f, -35f)
        quadToRelative(50f, 0f, 85f, 35f)
        reflectiveQuadToRelative(35f, 85f)
        quadToRelative(0f, 50f, -35f, 85f)
        reflectiveQuadToRelative(-85f, 35f)
        close()
        moveToRelative(0f, -80f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(520f, 480f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(480f, 440f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(440f, 480f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(480f, 520f)
        close()
        moveToRelative(280f, 80f)
        quadToRelative(-50f, 0f, -85f, -35f)
        reflectiveQuadToRelative(-35f, -85f)
        quadToRelative(0f, -50f, 35f, -85f)
        reflectiveQuadToRelative(85f, -35f)
        quadToRelative(50f, 0f, 85f, 35f)
        reflectiveQuadToRelative(35f, 85f)
        quadToRelative(0f, 50f, -35f, 85f)
        reflectiveQuadToRelative(-85f, 35f)
        close()
    }.also { _steppers = it }

private var _steppers: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Steppers)

