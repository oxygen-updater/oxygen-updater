package com.oxygenupdater.ui.common

import android.view.MotionEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.shimmer
import com.oxygenupdater.compose.ui.theme.backgroundVariant
import kotlin.math.ceil

// TODO(compose): switch to https://github.com/fornewid/placeholder
/**
 * @param textStyle if not null, it assumes composable is a Text, and uses [textShape] over [RoundedCornerShape]
 */
@Suppress("DEPRECATION")
fun Modifier.withPlaceholder(
    refreshing: Boolean,
    textStyle: TextStyle? = null,
) = if (!refreshing) this else composed(debugInspectorInfo {
    name = "withPlaceholder"
    properties["refreshing"] = refreshing
    properties["textStyle"] = textStyle
}) {
    placeholder(
        refreshing,
        shape = if (textStyle != null) {
            val density = LocalDensity.current
            remember(density, textStyle) {
                textShape(density, fontSize = textStyle.fontSize, lineHeight = textStyle.lineHeight)
            }
        } else RoundedCornerShape(4.dp),
        highlight = PlaceholderHighlight.shimmer(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
    )
}

fun Modifier.animatedClickable(
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
) = if (!enabled || onClick == null) this else composed(debugInspectorInfo {
    name = "animatedClickable"
    properties["enabled"] = enabled
    properties["onClick"] = onClick
}) {
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        scale, spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ScaleAnimation"
    )

    @OptIn(ExperimentalComposeUiApi::class)
    clickable(onClick = onClick)
        .motionEventSpy {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> scale = 0.95f
                MotionEvent.ACTION_UP -> scale = 1f
            }
        }
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
}

fun Modifier.borderExceptTop(color: Color) = drawWithCache {
    onDrawBehind {
        val stroke = ceil(1.dp.value)
        val size = size
        val width = size.width - stroke
        val height = size.height - stroke

        // Left
        drawLine(color, Offset(stroke, 0f), Offset(stroke, height), stroke)
        // Bottom
        drawLine(color, Offset(stroke, height), Offset(width, height), stroke * 2)
        // Right
        drawLine(color, Offset(width, height), Offset(width, stroke), stroke)
    }
}
