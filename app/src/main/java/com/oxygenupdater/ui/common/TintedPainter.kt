package com.oxygenupdater.ui.common

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * Create and return a new [Painter] that wraps [painter] with its [colorFilter].
 *
 * Simplified version of [this gist](https://gist.github.com/colinrtwhite/c2966e0b8584b4cdf0a5b05786b20ae1).
 */
fun tintedPainter(
    painter: Painter,
    colorFilter: ColorFilter,
): Painter = TintedPainter(painter, colorFilter)

private class TintedPainter(
    private val painter: Painter,
    private var colorFilter: ColorFilter?,
) : Painter() {

    override val intrinsicSize get() = painter.intrinsicSize

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() = with(painter) {
        draw(size, colorFilter = colorFilter)
    }
}
