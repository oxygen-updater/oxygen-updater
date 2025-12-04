package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.Commit: ImageVector
    get() = _commit ?: materialSymbol(
        name = "Commit",
    ) {
        moveTo(480f, 680f)
        quadToRelative(-73f, 0f, -127.5f, -45.5f)
        reflectiveQuadTo(284f, 520f)
        horizontalLineTo(80f)
        verticalLineToRelative(-80f)
        horizontalLineToRelative(204f)
        quadToRelative(14f, -69f, 68.5f, -114.5f)
        reflectiveQuadTo(480f, 280f)
        quadToRelative(73f, 0f, 127.5f, 45.5f)
        reflectiveQuadTo(676f, 440f)
        horizontalLineToRelative(204f)
        verticalLineToRelative(80f)
        horizontalLineTo(676f)
        quadToRelative(-14f, 69f, -68.5f, 114.5f)
        reflectiveQuadTo(480f, 680f)
        close()
        moveToRelative(0f, -80f)
        quadToRelative(50f, 0f, 85f, -35f)
        reflectiveQuadToRelative(35f, -85f)
        quadToRelative(0f, -50f, -35f, -85f)
        reflectiveQuadToRelative(-85f, -35f)
        quadToRelative(-50f, 0f, -85f, 35f)
        reflectiveQuadToRelative(-35f, 85f)
        quadToRelative(0f, 50f, 35f, 85f)
        reflectiveQuadToRelative(85f, 35f)
        close()
    }.also { _commit = it }

private var _commit: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.Commit)
