package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.TripOrigin: ImageVector
    get() = _tripOrigin ?: materialSymbol(
        name = "TripOrigin",
    ) {
        moveTo(480f, 880f)
        quadToRelative(-82f, 0f, -155f, -31.5f)
        reflectiveQuadToRelative(-127.5f, -86f)
        quadTo(143f, 708f, 111.5f, 635f)
        reflectiveQuadTo(80f, 480f)
        quadToRelative(0f, -83f, 31.5f, -155.5f)
        reflectiveQuadToRelative(86f, -127f)
        quadTo(252f, 143f, 325f, 111.5f)
        reflectiveQuadTo(480f, 80f)
        quadToRelative(83f, 0f, 155.5f, 31.5f)
        reflectiveQuadToRelative(127f, 86f)
        quadToRelative(54.5f, 54.5f, 86f, 127f)
        reflectiveQuadTo(880f, 480f)
        quadToRelative(0f, 82f, -31.5f, 155f)
        reflectiveQuadToRelative(-86f, 127.5f)
        quadToRelative(-54.5f, 54.5f, -127f, 86f)
        reflectiveQuadTo(480f, 880f)
        close()
        moveToRelative(0f, -160f)
        quadToRelative(100f, 0f, 170f, -70f)
        reflectiveQuadToRelative(70f, -170f)
        quadToRelative(0f, -100f, -70f, -170f)
        reflectiveQuadToRelative(-170f, -70f)
        quadToRelative(-100f, 0f, -170f, 70f)
        reflectiveQuadToRelative(-70f, 170f)
        quadToRelative(0f, 100f, 70f, 170f)
        reflectiveQuadToRelative(170f, 70f)
        close()
    }.also { _tripOrigin = it }

private var _tripOrigin: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.TripOrigin)
