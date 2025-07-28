package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.TrackChanges: ImageVector
    get() = _trackChanges ?: materialSymbol(
        name = "TrackChanges",
    ) {
        moveTo(480f, 880f)
        quadToRelative(-83f, 0f, -156f, -31.5f)
        reflectiveQuadTo(197f, 763f)
        quadToRelative(-54f, -54f, -85.5f, -127f)
        reflectiveQuadTo(80f, 480f)
        quadToRelative(0f, -83f, 31.5f, -156f)
        reflectiveQuadTo(197f, 197f)
        quadToRelative(54f, -54f, 127f, -85.5f)
        reflectiveQuadTo(480f, 80f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(520f, 120f)
        verticalLineToRelative(291f)
        quadToRelative(18f, 11f, 29f, 28.5f)
        reflectiveQuadToRelative(11f, 40.5f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(480f, 560f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(400f, 480f)
        quadToRelative(0f, -23f, 11f, -41f)
        reflectiveQuadToRelative(29f, -28f)
        verticalLineToRelative(-86f)
        quadToRelative(-52f, 14f, -86f, 56.5f)
        reflectiveQuadTo(320f, 480f)
        quadToRelative(0f, 66f, 47f, 113f)
        reflectiveQuadToRelative(113f, 47f)
        quadToRelative(66f, 0f, 113f, -47f)
        reflectiveQuadToRelative(47f, -113f)
        quadToRelative(0f, -26f, -7.5f, -48f)
        reflectiveQuadTo(612f, 390f)
        quadToRelative(-10f, -14f, -9f, -31f)
        reflectiveQuadToRelative(12f, -28f)
        quadToRelative(12f, -12f, 28.5f, -12f)
        reflectiveQuadToRelative(26.5f, 14f)
        quadToRelative(23f, 31f, 36.5f, 68f)
        reflectiveQuadToRelative(13.5f, 79f)
        quadToRelative(0f, 100f, -70f, 170f)
        reflectiveQuadToRelative(-170f, 70f)
        quadToRelative(-100f, 0f, -170f, -70f)
        reflectiveQuadToRelative(-70f, -170f)
        quadToRelative(0f, -90f, 57f, -156.5f)
        reflectiveQuadTo(440f, 243f)
        verticalLineToRelative(-81f)
        quadToRelative(-119f, 15f, -199.5f, 105f)
        reflectiveQuadTo(160f, 480f)
        quadToRelative(0f, 134f, 93f, 227f)
        reflectiveQuadToRelative(227f, 93f)
        quadToRelative(134f, 0f, 227f, -93f)
        reflectiveQuadToRelative(93f, -227f)
        quadToRelative(0f, -58f, -19f, -109.5f)
        reflectiveQuadTo(727f, 277f)
        quadToRelative(-11f, -13f, -11f, -30f)
        reflectiveQuadToRelative(12f, -29f)
        quadToRelative(12f, -12f, 28.5f, -11.5f)
        reflectiveQuadTo(784f, 220f)
        quadToRelative(45f, 53f, 70.5f, 119f)
        reflectiveQuadTo(880f, 480f)
        quadToRelative(0f, 83f, -31.5f, 156f)
        reflectiveQuadTo(763f, 763f)
        quadToRelative(-54f, 54f, -127f, 85.5f)
        reflectiveQuadTo(480f, 880f)
        close()
    }.also { _trackChanges = it }

private var _trackChanges: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.TrackChanges)
