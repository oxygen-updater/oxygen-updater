package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Search: ImageVector
    get() = _search ?: materialSymbol(
        name = "Search",
    ) {
        moveTo(380.0f, 640.0f)
        quadToRelative(-109.0f, 0.0f, -184.5f, -75.5f)
        reflectiveQuadTo(120.0f, 380.0f)
        quadToRelative(0.0f, -109.0f, 75.5f, -184.5f)
        reflectiveQuadTo(380.0f, 120.0f)
        quadToRelative(109.0f, 0.0f, 184.5f, 75.5f)
        reflectiveQuadTo(640.0f, 380.0f)
        quadToRelative(0.0f, 44.0f, -14.0f, 83.0f)
        reflectiveQuadToRelative(-38.0f, 69.0f)
        lineToRelative(224.0f, 224.0f)
        quadToRelative(11.0f, 11.0f, 11.0f, 28.0f)
        reflectiveQuadToRelative(-11.0f, 28.0f)
        quadToRelative(-11.0f, 11.0f, -28.0f, 11.0f)
        reflectiveQuadToRelative(-28.0f, -11.0f)
        lineTo(532.0f, 588.0f)
        quadToRelative(-30.0f, 24.0f, -69.0f, 38.0f)
        reflectiveQuadToRelative(-83.0f, 14.0f)
        close()
        moveTo(380.0f, 560.0f)
        quadToRelative(75.0f, 0.0f, 127.5f, -52.5f)
        reflectiveQuadTo(560.0f, 380.0f)
        quadToRelative(0.0f, -75.0f, -52.5f, -127.5f)
        reflectiveQuadTo(380.0f, 200.0f)
        quadToRelative(-75.0f, 0.0f, -127.5f, 52.5f)
        reflectiveQuadTo(200.0f, 380.0f)
        quadToRelative(0.0f, 75.0f, 52.5f, 127.5f)
        reflectiveQuadTo(380.0f, 560.0f)
        close()
    }.also { _search = it }

private var _search: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Search)
