package com.oxygenupdater.compose.ui

import android.view.animation.AccelerateInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.theme.backgroundVariant
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TopAppBar(
    behavior: TopAppBarScrollBehavior,
    onNavIconClick: () -> Unit,
    subtitle: String,
    modifier: Modifier = Modifier,
    root: Boolean = true,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val state = remember { behavior.state }

    val heightOffset = with(LocalDensity.current) { state.heightOffset.toDp() }
    val height = (TopAppBarHeight + heightOffset).coerceAtLeast(0.dp)

    val appBarModifier = modifier
        .padding(AppBarDefaults.ContentPadding)
        .fillMaxWidth()
        // Set up support for resizing the top app bar when vertically dragging the bar itself
        .draggable(rememberDraggableState {
            behavior.state.heightOffset += it
        }, Orientation.Vertical, onDragStopped = {
            settleAppBar(behavior.state, it, behavior.flingAnimationSpec, behavior.snapAnimationSpec)
        })

    Surface {
        Column {
            Row(appBarModifier.height(height), verticalAlignment = Alignment.CenterVertically) {
                // Nav icon
                IconButton(onNavIconClick, Modifier.padding(end = 4.dp)) {
                    Icon(
                        if (root) CustomIcons.LogoNotification else Icons.Rounded.ArrowBack,
                        stringResource(if (root) R.string.about else androidx.appcompat.R.string.abc_action_bar_up_description),
                        Modifier.requiredSize(24.dp),
                        MaterialTheme.colors.primary
                    )
                }

                // Title & subtitle
                Column(Modifier.weight(1f), Arrangement.Center) {
                    Text(
                        stringResource(R.string.app_name),
                        overflow = TextOverflow.Ellipsis, maxLines = 1,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        subtitle,
                        overflow = TextOverflow.Ellipsis, maxLines = 1,
                        style = MaterialTheme.typography.body2
                    )
                }

                if (actions != null) actions()
            }

            if (height != 0.dp) Divider()
        }
    }
}

@Composable
fun CollapsingAppBar(
    behavior: TopAppBarScrollBehavior,
    image: @Composable (BoxWithConstraintsScope.(Modifier) -> Unit),
    title: String,
    subtitle: String? = null,
    onNavIconClick: (() -> Unit)? = null,
) {
    val (minHeight, maxHeight) = remember { CollapsingAppBarHeight }
    val (minTitleSize, maxTitleSize) = remember { CollapsingAppBarTitleSize }
    val (minSubtitleSize, maxSubtitleSize) = remember { CollapsingAppBarSubtitleSize }

    val state = remember { behavior.state }
    val transitionFraction = state.collapsedFraction

    val statusBarHeightPx: Int
    val heightOffset: Dp
    val heightOffsetPx = state.heightOffset
    val minHeightPx: Float
    val maxHeightPx: Float
    val bottomPaddingPx: Int
    with(LocalDensity.current) {
        statusBarHeightPx = WindowInsets.statusBars.getTop(this)
        heightOffset = heightOffsetPx.toDp()

        minHeightPx = remember { minHeight.toPx() }
        maxHeightPx = remember { maxHeight.toPx() }
        bottomPaddingPx = remember { CollapsingAppBarBottomPadding.roundToPx() }
    }

    val height = (maxHeight + heightOffset).coerceAtLeast(minHeight)
    val heightPx = (maxHeightPx + heightOffsetPx).coerceAtLeast(minHeightPx)

    // Sets the app bar's height offset limit to hide just the bottom title area and keep top title
    // visible when collapsed.
    SideEffect {
        val limit = minHeightPx - maxHeightPx + statusBarHeightPx
        if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
    }

    // Set up support for resizing the top app bar when vertically dragging the bar itself
    val appBarModifier = Modifier.draggable(rememberDraggableState {
        behavior.state.heightOffset += it
    }, Orientation.Vertical, onDragStopped = {
        settleAppBar(behavior.state, it, behavior.flingAnimationSpec, behavior.snapAnimationSpec)
    })

    Layout({
        BoxWithConstraints(Modifier.layoutId(LayoutIdImage)) {
            val size = Modifier.size(maxWidth, height)
            val colors = MaterialTheme.colors
            val hover = remember { colors.backgroundVariant }
            val surface = remember { colors.surface }
            // Lerp from 75% opacity hover to 100% opacity surface
            val lerpColor = Color(
                red = lerp(hover.red, surface.red, transitionFraction),
                green = lerp(hover.green, surface.green, transitionFraction),
                blue = lerp(hover.blue, surface.blue, transitionFraction),
                alpha = lerp(.75f, 1f, transitionFraction),
            )

            image(size)
            Box(size.background(lerpColor))
            if (height != maxHeight) Divider(Modifier.align(Alignment.BottomStart))
        }

        if (onNavIconClick != null) IconButton(
            onNavIconClick,
            Modifier
                .layoutId(LayoutIdNavIcon)
                .height(minHeight)
                .padding(horizontal = 4.dp),
        ) {
            Icon(Icons.Rounded.ArrowBack, stringResource(androidx.appcompat.R.string.abc_action_bar_up_description))
        }

        Column(
            Modifier
                .layoutId(LayoutIdText)
                .padding(
                    start = if (onNavIconClick != null) {
                        // Lerp only if there's a nav icon
                        lerp(20f, 56f, transitionFraction).dp
                    } else 16.dp, end = 16.dp
                )
        ) {
            val typography = MaterialTheme.typography
            Text(
                title,
                overflow = TextOverflow.Ellipsis, maxLines = lerp(3, 1, transitionFraction),
                style = typography.h5.copy(
                    fontSize = lerp(maxTitleSize, minTitleSize, transitionFraction).sp
                )
            )

            if (subtitle != null) Text(
                subtitle,
                overflow = TextOverflow.Ellipsis, maxLines = 1,
                style = typography.body1.copy(
                    fontSize = lerp(maxSubtitleSize, minSubtitleSize, transitionFraction).sp
                )
            )
        }
    }, appBarModifier) { measurables, constraints ->
        var imagePlaceable: Placeable? = null
        var navIconPlaceable: Placeable? = null
        var textPlaceable: Placeable? = null
        for (it in measurables) {
            when (it.layoutId) {
                LayoutIdImage -> imagePlaceable = it.measure(constraints)
                LayoutIdNavIcon -> navIconPlaceable = it.measure(constraints)
                LayoutIdText -> textPlaceable = it.measure(constraints)
            }
        }

        val layoutHeight = heightPx.roundToInt()
        layout(constraints.maxWidth, layoutHeight) {
            imagePlaceable?.placeRelative(0, 0)
            navIconPlaceable?.placeRelative(0, statusBarHeightPx)

            textPlaceable?.let {
                val base = layoutHeight - it.height
                val bottom = base - bottomPaddingPx
                val center = (base + statusBarHeightPx) shr 1
                // We're interpolating from bottom to center, so slowing down the start is necessary
                // to make title appear to always be at the bottom (but also centered when collapsed)
                it.placeRelative(0, lerp(bottom, center, accelerateInterpolator.getInterpolation(transitionFraction)))
            }
        }
    }
}

private const val LayoutIdImage = "image"
private const val LayoutIdNavIcon = "navigationIcon"
private const val LayoutIdText = "text"

/** we're adding to M2's default of 56 to make space for subtitle */
private val TopAppBarHeight = 64.dp

/** min (M3 attribute `?actionBarSize`) to max */
private val CollapsingAppBarHeight = 64.dp to 256.dp

/** same as LargeTitleBottomPadding in AppBar.kt */
private val CollapsingAppBarBottomPadding = 28.dp

/** min (h6) to max (h5) */
private val CollapsingAppBarTitleSize = 20f to 24f

/** min (body2) to max (body1) */
private val CollapsingAppBarSubtitleSize = 14f to 16f

private val accelerateInterpolator = AccelerateInterpolator(3f)

/**
 * Monotonic but imprecise linear interpolation between `startValue` and `endValue` by `fraction`
 *
 * @see <a href="https://en.wikipedia.org/wiki/Linear_interpolation#Programming_language_support">Linear interpolation — Wikipedia</a>
 */
private fun lerp(startValue: Float, endValue: Float, fraction: Float) = startValue + fraction * (endValue - startValue)
private fun lerp(startValue: Int, endValue: Int, fraction: Float) = startValue + (fraction * (endValue - startValue)).roundToInt()

/**
 * Everything beyond this point has been taken from
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt
 */

/**
 * A TopAppBarScrollBehavior defines how an app bar should behave when the content under it is
 * scrolled.
 *
 * @see [TopAppBarDefaults.pinnedScrollBehavior]
 * @see [TopAppBarDefaults.enterAlwaysScrollBehavior]
 * @see [TopAppBarDefaults.exitUntilCollapsedScrollBehavior]
 */
@Stable
interface TopAppBarScrollBehavior {
    /**
     * A [TopAppBarState] that is attached to this behavior and is read and updated when scrolling
     * happens.
     */
    val state: TopAppBarState

    /**
     * An optional [AnimationSpec] that defines how the top app bar snaps to either fully collapsed
     * or fully extended state when a fling or a drag scrolled it into an intermediate position.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * An optional [DecayAnimationSpec] that defined how to fling the top app bar when the user
     * flings the app bar itself, or the content below it.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>?

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll][androidx.compose.ui.input.nestedscroll.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection
}

/** Contains default values used for the top app bar implementations. */
object TopAppBarDefaults {

    /**
     * Returns a [TopAppBarScrollBehavior]. A top app bar that is set up with this
     * [TopAppBarScrollBehavior] will immediately collapse when the content is pulled up, and will
     * immediately appear when the content is pulled down.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     * state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be
     * handled by this [EnterAlwaysScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps
     * to either fully collapsed or fully extended state when a fling or a drag scrolled it into an
     * intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top
     * app bar when the user flings the app bar itself, or the content below it
     */
    @Composable
    fun enterAlwaysScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(
            // Sets the app bar's height offset limit to just hide itself completely
            with(LocalDensity.current) { -TopAppBarHeight.toPx() }
        ),
        canScroll: () -> Boolean = { true },
        snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): TopAppBarScrollBehavior = EnterAlwaysScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll
    )

    /**
     * Returns a [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and
     * height of the top app bar.
     *
     * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse
     * when the nested content is pulled up, and will expand back the collapsed area when the
     * content is  pulled all the way down.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     * state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be
     * handled by this [ExitUntilCollapsedScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps
     * to either fully collapsed or fully extended state when a fling or a drag scrolled it into an
     * intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top
     * app bar when the user flings the app bar itself, or the content below it
     */
    @Composable
    fun exitUntilCollapsedScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(),
        canScroll: () -> Boolean = { true },
        snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): TopAppBarScrollBehavior = ExitUntilCollapsedScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll
    )
}

/**
 * Creates a [TopAppBarState] that is remembered across compositions.
 *
 * @param initialHeightOffsetLimit the initial value for [TopAppBarState.heightOffsetLimit],
 * which represents the pixel limit that a top app bar is allowed to collapse when the scrollable
 * content is scrolled
 * @param initialHeightOffset the initial value for [TopAppBarState.heightOffset]. The initial
 * offset height offset should be between zero and [initialHeightOffsetLimit].
 * @param initialContentOffset the initial value for [TopAppBarState.contentOffset]
 */
@Composable
fun rememberTopAppBarState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
) = rememberSaveable(saver = TopAppBarState.Saver) {
    TopAppBarState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
}

/**
 * A state object that can be hoisted to control and observe the top app bar state. The state is
 * read and updated by a [TopAppBarScrollBehavior] implementation.
 *
 * In most cases, this state will be created via [rememberTopAppBarState].
 *
 * @param initialHeightOffsetLimit the initial value for [TopAppBarState.heightOffsetLimit]
 * @param initialHeightOffset the initial value for [TopAppBarState.heightOffset]
 * @param initialContentOffset the initial value for [TopAppBarState.contentOffset]
 */
@Stable
class TopAppBarState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
) {
    /**
     * The top app bar's height offset limit in pixels, which represents the limit that a top app
     * bar is allowed to collapse to.
     *
     * Use this limit to coerce the [heightOffset] value when it's updated.
     */
    var heightOffsetLimit by mutableFloatStateOf(initialHeightOffsetLimit)

    /**
     * The top app bar's current height offset in pixels. This height offset is applied to the fixed
     * height of the app bar to control the displayed height when content is being scrolled.
     *
     * Updates to the [heightOffset] value are coerced between zero and [heightOffsetLimit].
     */
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue = newOffset.coerceIn(heightOffsetLimit, 0f)
        }

    /**
     * The total offset of the content scrolled under the top app bar.
     *
     * The content offset is used to compute the [overlappedFraction], which can later be read
     * by an implementation.
     *
     * This value is updated by a [TopAppBarScrollBehavior] whenever a nested scroll connection
     * consumes scroll events. A common implementation would update the value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed.y` values.
     */
    var contentOffset by mutableFloatStateOf(initialContentOffset)

    /**
     * A value that represents the collapsed height percentage of the app bar.
     *
     * A `0.0` represents a fully expanded bar, and `1.0` represents a fully collapsed bar (computed
     * as [heightOffset] / [heightOffsetLimit]).
     */
    val collapsedFraction: Float
        get() = if (heightOffsetLimit != 0f) heightOffset / heightOffsetLimit else 0f

    companion object {
        /**
         * The default [Saver] implementation for [TopAppBarState].
         */
        val Saver: Saver<TopAppBarState, *> = listSaver(
            save = { listOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset) },
            restore = {
                TopAppBarState(
                    initialHeightOffsetLimit = it[0],
                    initialHeightOffset = it[1],
                    initialContentOffset = it[2]
                )
            }
        )
    }

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will immediately appear when the content is pulled down.
 *
 * @param state a [TopAppBarState]
 * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps to
 * either fully collapsed or fully extended state when a fling or a drag scrolled it into an
 * intermediate position
 * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top app
 * bar when the user flings the app bar itself, or the content below it
 * @param canScroll a callback used to determine whether scroll events are to be
 * handled by this [EnterAlwaysScrollBehavior]
 */
private class EnterAlwaysScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override var nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!canScroll()) return Offset.Zero
            val prevHeightOffset = state.heightOffset
            state.heightOffset = state.heightOffset + available.y
            return if (prevHeightOffset != state.heightOffset) {
                // We're in the middle of top app bar collapse or expand.
                // Consume only the scroll on the Y axis.
                available.copy(x = 0f)
            } else Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!canScroll()) return Offset.Zero
            state.contentOffset += consumed.y
            if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                // Reset the total content offset to zero when scrolling all the way down.
                // This will eliminate some float precision inaccuracies.
                if (consumed.y == 0f && available.y > 0f) state.contentOffset = 0f
            }
            state.heightOffset = state.heightOffset + consumed.y
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val superConsumed = super.onPostFling(consumed, available)
            return superConsumed + settleAppBar(state, available.y, flingAnimationSpec, snapAnimationSpec)
        }
    }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will expand back the collapsed area when the content is
 * pulled all the way down.
 *
 * @param state a [TopAppBarState]
 * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps to
 * either fully collapsed or fully extended state when a fling or a drag scrolled it into an
 * intermediate position
 * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top app
 * bar when the user flings the app bar itself, or the content below it
 * @param canScroll a callback used to determine whether scroll events are to be
 * handled by this [ExitUntilCollapsedScrollBehavior]
 */
private class ExitUntilCollapsedScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override var nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Don't intercept if scrolling down.
            if (!canScroll() || available.y > 0f) return Offset.Zero
            val prevHeightOffset = state.heightOffset
            state.heightOffset = state.heightOffset + available.y
            return if (prevHeightOffset != state.heightOffset) {
                // We're in the middle of top app bar collapse or expand.
                // Consume only the scroll on the Y axis.
                available.copy(x = 0f)
            } else Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!canScroll()) return Offset.Zero
            state.contentOffset += consumed.y
            if (available.y < 0f || consumed.y < 0f) {
                // When scrolling up, just update the state's height offset.
                val oldHeightOffset = state.heightOffset
                state.heightOffset = state.heightOffset + consumed.y
                return Offset(0f, state.heightOffset - oldHeightOffset)
            }

            // Reset the total content offset to zero when scrolling all the way down. This
            // will eliminate some float precision inaccuracies.
            if (consumed.y == 0f && available.y > 0) state.contentOffset = 0f

            if (available.y > 0f) {
                // Adjust the height offset in case the consumed delta Y is less than what was
                // recorded as available delta Y in the pre-scroll.
                val oldHeightOffset = state.heightOffset
                state.heightOffset = state.heightOffset + available.y
                return Offset(0f, state.heightOffset - oldHeightOffset)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val superConsumed = super.onPostFling(consumed, available)
            return superConsumed + settleAppBar(state, available.y, flingAnimationSpec, snapAnimationSpec)
        }
    }
}

private suspend fun settleAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) return Velocity.Zero

    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(0f, velocity).animateDecay(flingAnimationSpec) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset
            state.heightOffset = initialHeightOffset + delta
            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // Avoid rounding errors and stop if anything is unconsumed
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
    }

    // Snap if animation specs were provided
    if (snapAnimationSpec != null && (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit)) {
        AnimationState(state.heightOffset).animateTo(
            if (state.collapsedFraction < 0.5f) 0f
            else state.heightOffsetLimit,
            animationSpec = snapAnimationSpec,
        ) { state.heightOffset = value }
    }

    return Velocity(0f, remainingVelocity)
}
