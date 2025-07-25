package com.oxygenupdater.ui.common.scrollbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.common.scrollbar.ThumbState.Companion.Active
import com.oxygenupdater.ui.common.scrollbar.ThumbState.Companion.Dormant
import com.oxygenupdater.ui.common.scrollbar.ThumbState.Companion.Inactive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [Scrollbar] that optionally allows for fast scrolling of content by dragging its thumb.
 * Its thumb disappears when the scrolling container is dormant.
 *
 * @param state the state describing the position of the scrollbar
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param orientation the scroll direction of the scrollbar (default [Vertical])
 * @param onThumbMoved (optional) function to allow fast scrolling behaviour, based on interactions
 * on the scrollbar thumb by the user
 */
@Composable
fun ScrollableState.Scrollbar(
    state: ScrollbarState,
    modifier: Modifier = Modifier,
    orientation: Orientation = Vertical,
    onThumbMoved: ((Float) -> Unit)? = null,
) {
    // Used to immediately show drag feedback in the UI while the scrolling implementation catches up
    var interactionThumbTravelPercent by rememberState(Float.NaN)

    var track by rememberState(ScrollbarTrack(0L))

    val interactionSource: MutableInteractionSource?
    val modifier = if (onThumbMoved == null) {
        interactionSource = null
        modifier
    } else {
        /** If [onThumbMoved] is set, caller wants press/drag behaviour for fast scrolling */
        /** If [onThumbMoved] is set, caller wants press/drag behaviour for fast scrolling */
        interactionSource = remember { MutableInteractionSource() }

        // Using Offset.Unspecified and Float.NaN instead of null
        // to prevent unnecessary boxing of primitives
        var pressedOffset by rememberState(Offset.Unspecified)
        var draggedOffset by rememberState(Offset.Unspecified)

        // Process presses
        LaunchedEffect(Unit) {
            snapshotFlow { pressedOffset }.collect { pressedOffset ->
                // Press ended, reset interactionThumbTravelPercent
                if (pressedOffset == Offset.Unspecified) {
                    interactionThumbTravelPercent = Float.NaN
                    return@collect
                }

                var currentThumbMovedPercent = state.thumbMovedPercent
                val destinationThumbMovedPercent = track.thumbPosition(orientation.valueOf(pressedOffset))
                val isPositive = currentThumbMovedPercent < destinationThumbMovedPercent
                val delta = ScrollbarPressDeltaPercent * if (isPositive) 1f else -1f

                while (currentThumbMovedPercent != destinationThumbMovedPercent) {
                    currentThumbMovedPercent = when {
                        isPositive -> min(currentThumbMovedPercent + delta, destinationThumbMovedPercent)
                        else -> max(currentThumbMovedPercent + delta, destinationThumbMovedPercent)
                    }

                    onThumbMoved(currentThumbMovedPercent)
                    interactionThumbTravelPercent = currentThumbMovedPercent
                    delay(ScrollbarPressDelayMs)
                }
            }
        }

        // Process drags
        LaunchedEffect(Unit) {
            snapshotFlow { draggedOffset }.collect { draggedOffset ->
                if (draggedOffset == Offset.Unspecified) {
                    interactionThumbTravelPercent = Float.NaN
                    return@collect
                }

                val currentTravel = track.thumbPosition(orientation.valueOf(draggedOffset))
                onThumbMoved(currentTravel)
                interactionThumbTravelPercent = currentTravel
            }
        }

        modifier
            // Process scrollbar presses
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        try {
                            // Wait for a long press before scrolling
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                tryAwaitRelease()
                            }
                        } catch (_: TimeoutCancellationException) {
                            // Start the press triggered scroll
                            val initialPress = PressInteraction.Press(offset)
                            interactionSource.tryEmit(initialPress)

                            pressedOffset = offset
                            interactionSource.tryEmit(
                                when {
                                    tryAwaitRelease() -> PressInteraction.Release(initialPress)
                                    else -> PressInteraction.Cancel(initialPress)
                                },
                            )

                            // End the press
                            pressedOffset = Offset.Unspecified
                        }
                    },
                )
            }
            // Process scrollbar drags
            .pointerInput(Unit) {
                var dragInteraction: DragInteraction.Start? = null
                val onDragStart: (Offset) -> Unit = { offset ->
                    val start = DragInteraction.Start()
                    dragInteraction = start
                    interactionSource.tryEmit(start)
                    draggedOffset = offset
                }
                val onDragEnd: () -> Unit = {
                    dragInteraction?.let { interactionSource.tryEmit(DragInteraction.Stop(it)) }
                    draggedOffset = Offset.Unspecified
                }
                val onDragCancel: () -> Unit = {
                    dragInteraction?.let { interactionSource.tryEmit(DragInteraction.Cancel(it)) }
                    draggedOffset = Offset.Unspecified
                }
                val onDrag: (change: PointerInputChange, dragAmount: Float) -> Unit = onDrag@{ _, delta ->
                    if (draggedOffset == Offset.Unspecified) return@onDrag

                    draggedOffset = when (orientation) {
                        Vertical -> draggedOffset.copy(y = draggedOffset.y + delta)
                        Horizontal -> draggedOffset.copy(x = draggedOffset.x + delta)
                    }
                }

                when (orientation) {
                    Horizontal -> detectHorizontalDragGestures(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onHorizontalDrag = onDrag,
                    )

                    Vertical -> detectVerticalDragGestures(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onVerticalDrag = onDrag,
                    )
                }
            }
    }

    // Scrollbar track container
    Box(
        modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.systemBars.let {
                    // Leave space for 2/3-button nav bar in landscape mode
                    when (orientation) {
                        Vertical -> it.only(WindowInsetsSides.Horizontal)
                        Horizontal -> it
                    }
                }
            )
            .padding(horizontal = 2.dp)
            .run {
                val withHover = interactionSource?.let(::hoverable) ?: this
                when (orientation) {
                    Vertical -> withHover.fillMaxHeight()
                    Horizontal -> withHover.fillMaxWidth()
                }
            }
            .onGloballyPositioned { coordinates ->
                val scrollbarStartCoordinate = orientation.valueOf(coordinates.positionInRoot())
                track = ScrollbarTrack(
                    max = scrollbarStartCoordinate,
                    min = scrollbarStartCoordinate + orientation.valueOf(coordinates.size),
                )
            }
    ) {
        // Scrollbar thumb container
        Layout({
            ScrollbarThumb(
                interactionSource = interactionSource,
                orientation = orientation,
                draggable = onThumbMoved != null,
            )
        }) { measurables, constraints ->
            val measurable = measurables.first()

            val thumbSizePx = max(state.thumbSizePercent * track.size, MinThumbSize.toPx())
            val trackSizePx = when (state.thumbTrackSizePercent) {
                0f -> track.size
                else -> (track.size - thumbSizePx) / state.thumbTrackSizePercent.fastCoerceAtLeast(1f)
            }

            val thumbTravelPercent = max(
                min(
                    when {
                        interactionThumbTravelPercent.isNaN() -> state.thumbMovedPercent
                        else -> interactionThumbTravelPercent
                    },
                    state.thumbTrackSizePercent,
                ),
                0f,
            )

            val thumbMovedPx = (trackSizePx * thumbTravelPercent).roundToInt()
            val y = when (orientation) {
                Horizontal -> 0
                Vertical -> thumbMovedPx
            }
            val x = when (orientation) {
                Horizontal -> thumbMovedPx
                Vertical -> 0
            }

            val thumbSizePxRounded = thumbSizePx.roundToInt()
            val updatedConstraints = when (orientation) {
                Horizontal -> constraints.copy(
                    minWidth = thumbSizePxRounded,
                    maxWidth = thumbSizePxRounded,
                )

                Vertical -> constraints.copy(
                    minHeight = thumbSizePxRounded,
                    maxHeight = thumbSizePxRounded,
                )
            }

            val placeable = measurable.measure(updatedConstraints)
            layout(placeable.width, placeable.height) {
                placeable.place(x, y)
            }
        }
    }
}

/**
 * A scrollbar thumb that is intended either be a tough target for fast scrolling, or simply to
 * communicate the user's position in the scrolling container.
 *
 * @param draggable adjusts size for fast scrolling touch target vs simply visual behaviour
 */
@Composable
private fun ScrollbarThumb(
    interactionSource: InteractionSource?,
    orientation: Orientation,
    draggable: Boolean,
) = Box(
    Modifier
        .run {
            val size = if (draggable) 6.dp else 2.dp
            when (orientation) {
                Vertical -> width(size).fillMaxHeight()
                Horizontal -> height(size).fillMaxWidth()
            }
        }
        .scrollThumb(interactionSource),
)

@Composable
private fun Modifier.scrollThumb(interactionSource: InteractionSource?): Modifier {
    val color = interactionSource?.let {
        scrollbarThumbColor(it).value
    } ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    return this then ScrollThumbElement { color }
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 *
 * A [ThumbState] of [Active] is defined as it being pressed, hovered, or dragged.
 *
 * @param interactionSource source of interactions in the scrolling container
 */
@Composable
private fun scrollbarThumbColor(
    interactionSource: InteractionSource,
): State<Color> {
    var state by rememberState(Inactive.value)

    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = pressed || hovered || dragged
    LaunchedEffect(active) {
        state = if (active) Active.value else Inactive.value

        // if (active) state = Active.value else if (state == Active.value) {
        //     // Auto-hide scrollbar after a fixed delay
        //     state = Inactive.value
        //     delay(ScrollbarInactiveToDormantTimeMs)
        //     state = Dormant.value
        // }
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    return animateColorAsState(
        targetValue = when (state) {
            Active.value -> onSurface.copy(0.24f)
            Inactive.value -> onSurface.copy(alpha = 0.12f) // dim
            Dormant.value -> Color.Transparent // hide
            else -> Color.Transparent
        },
        animationSpec = SpringSpec(stiffness = Spring.StiffnessLow),
        label = "ScrollbarThumbColor",
    )
}

private data class ScrollThumbElement(
    val colorProducer: ColorProducer,
) : ModifierNodeElement<ScrollThumbNode>() {

    override fun create() = ScrollThumbNode(colorProducer)

    override fun update(node: ScrollThumbNode) {
        node.colorProducer = colorProducer
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollbarThumb"
        properties["colorProducer"] = colorProducer
    }
}

private class ScrollThumbNode(var colorProducer: ColorProducer) : DrawModifierNode, Modifier.Node() {

    private val shape = RoundedCornerShape(16.dp)

    // Naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        val outline = if (size == lastSize && layoutDirection == lastLayoutDirection) {
            lastOutline!!
        } else shape.createOutline(size, layoutDirection, this)

        val color = colorProducer()
        if (color != Color.Unspecified) drawOutline(outline, color)

        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
    }
}

class ScrollbarState {

    private var packedValue by mutableLongStateOf(0L)

    fun onScroll(stateValue: ScrollbarStateValue) {
        packedValue = stateValue.packedValue
    }

    /**
     * Returns the thumb size of the scrollbar as a percentage of the total track size
     */
    val thumbSizePercent
        get() = unpackFloat1(packedValue)

    /**
     * Returns the distance the thumb has traveled as a percentage of total track size
     */
    val thumbMovedPercent
        get() = unpackFloat2(packedValue)

    /**
     * Returns the max distance the thumb can travel as a percentage of total track size
     */
    val thumbTrackSizePercent
        get() = 1f - thumbSizePercent
}

/**
 * Returns the size of the scrollbar track in pixels
 */
private val ScrollbarTrack.size
    get() = unpackFloat2(packedValue) - unpackFloat1(packedValue)

/**
 * Returns the position of the scrollbar thumb on the track as a percentage
 */
private fun ScrollbarTrack.thumbPosition(dimension: Float) = max(min(dimension / size.fastCoerceAtLeast(1f), 1f), 0f)

/**
 * Class definition for the core properties of a scroll bar
 */
@Immutable
@JvmInline
value class ScrollbarStateValue(val packedValue: Long)

/**
 * Class definition for the core properties of a scroll bar track
 */
@Immutable
@JvmInline
private value class ScrollbarTrack(val packedValue: Long) {
    constructor(max: Float, min: Float) : this(packFloats(max, min))
}

@Immutable
@JvmInline
value class ThumbState private constructor(val value: Int) {

    override fun toString() = "ThumbState." + when (this) {
        Active -> "Active"
        Inactive -> "Inactive"
        Dormant -> "Dormant"
        else -> "Invalid"
    }

    companion object {
        val Active = ThumbState(0)
        val Inactive = ThumbState(1)
        val Dormant = ThumbState(2)
    }
}

/**
 * Creates a [ScrollbarStateValue] with the listed properties.
 *
 * @param thumbSizePercent the thumb size of the scrollbar as a percentage of the total track size.
 *  Refers to either the thumb width (for horizontal scrollbars)
 *  or height (for vertical scrollbars).
 * @param thumbMovedPercent the distance the thumb has traveled as a percentage of total
 * track size.
 */
fun scrollbarStateValue(
    thumbSizePercent: Float,
    thumbMovedPercent: Float,
) = ScrollbarStateValue(packFloats(thumbSizePercent, thumbMovedPercent))

/**
 * Returns the value of [offset] along the axis specified by [this]
 */
fun Orientation.valueOf(offset: Offset) = when (this) {
    Horizontal -> offset.x
    Vertical -> offset.y
}

/**
 * Returns the value of [intSize] along the axis specified by [this]
 */
fun Orientation.valueOf(intSize: IntSize) = when (this) {
    Horizontal -> intSize.width
    Vertical -> intSize.height
}

/**
 * Returns the value of [intOffset] along the axis specified by [this]
 */
fun Orientation.valueOf(intOffset: IntOffset) = when (this) {
    Horizontal -> intOffset.x
    Vertical -> intOffset.y
}

/**
 * The time period for showing the scrollbar thumb after interacting with it, before it fades away
 */
private const val ScrollbarInactiveToDormantTimeMs = 2_000L

/**
 * The delay between scrolls when a user long presses on the scrollbar track to initiate a scroll
 * instead of dragging the scrollbar thumb.
 */
private const val ScrollbarPressDelayMs = 10L

/**
 * The percentage displacement of the scrollbar when scrolled by long presses on the scrollbar
 * track.
 */
private const val ScrollbarPressDeltaPercent = 0.02f

private val MinThumbSize = 40.dp
