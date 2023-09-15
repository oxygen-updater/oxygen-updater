package com.oxygenupdater.ui.common

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.abs
import kotlin.math.floor

/**
 * Generate a full-width rounded rectangle for text subtracting its approximate top & bottom padding
 *
 * Meant to be used with `Modifier.`[withPlaceholder]
 */
fun textShape(density: Density, fontSize: TextUnit, lineHeight: TextUnit): Shape {
    val lineHeightPx: Float
    val fontSizePx: Float
    val cornerRadius: CornerRadius
    density.run {
        lineHeightPx = lineHeight.run { if (isSpecified) toPx() else 1f }
        fontSizePx = fontSize.run { if (isSpecified) toPx() else 1f }
        cornerRadius = CornerRadius(4.dp.toPx())
    }

    val padding = abs(lineHeightPx - fontSizePx) / 2
    return GenericShape { size, _ ->
        addRoundRect(RoundRect(0f, padding, size.width, size.height - padding, cornerRadius))
    }
}

/**
 * Generate rounded rectangles for each approximate line (full-width) in text
 *
 * Meant to be used with `Modifier.`[withPlaceholder]
 */
fun perLineTextShape(density: Density, fontSize: TextUnit, lineHeight: TextUnit): Shape {
    val lineHeightPx: Float
    val fontSizePx: Float
    val cornerRadius: CornerRadius
    density.run {
        lineHeightPx = lineHeight.run { if (isSpecified) toPx() else 1f }
        fontSizePx = fontSize.run { if (isSpecified) toPx() else 1f }
        cornerRadius = CornerRadius(4.dp.toPx())
    }

    val offset = Offset(0f, abs(lineHeightPx - fontSizePx) / 2)
    return GenericShape { size, _ ->
        val lineSize = size.copy(size.width, fontSizePx)
        val rect = RoundRect(Rect(offset, lineSize), cornerRadius)

        val lines = floor(size.height / lineHeightPx).toInt()
        repeat(lines) {
            addRoundRect(rect.translate(Offset(0f, lineHeightPx * it)))
        }
    }
}
