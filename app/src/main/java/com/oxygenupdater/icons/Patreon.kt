package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Logos.Patreon: ImageVector
    get() = _patreon ?: materialIcon("Patreon") {
        // Circle
        moveTo(14.8f, 9.6f)
        moveToRelative(-7.2f, 0f)
        arcToRelative(7.2f, 7.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 14.4f, dy1 = 0f)
        arcToRelative(7.2f, 7.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -14.4f, dy1 = 0f)
        // Line
        moveTo(2f, 2.4f)
        horizontalLineToRelative(3.5f)
        verticalLineToRelative(19.2f)
        horizontalLineToRelative(-3.5f)
        close()
    }.also { _patreon = it }

private var _patreon: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Logos.Patreon)
