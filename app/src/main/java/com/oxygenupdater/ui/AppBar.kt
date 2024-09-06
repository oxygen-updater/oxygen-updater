package com.oxygenupdater.ui

import android.annotation.SuppressLint
import android.view.animation.AccelerateInterpolator
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.ui.common.rememberSaveableState
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    @StringRes subtitleResId: Int,
    root: Boolean,
    onNavIconClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) = Column {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val statusBarHeightPx: Int
    val heightPx: Int
    with(LocalDensity.current) {
        statusBarHeightPx = WindowInsets.statusBars.getTop(this)
        heightPx = CollapsingAppBarHeight.first.roundToPx() + statusBarHeightPx
    }

    // Sets the app bar's height offset to collapse the entire bar's height when content is
    // scrolled.
    val layoutHeight = (heightPx + scrollBehavior.state.heightOffset.toInt()).coerceAtLeast(0)
    val limit = -heightPx.toFloat()
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != limit) scrollBehavior.state.heightOffsetLimit = limit
    }

    // Sometimes it goes out of boundary, don't know why. Force within [0,1].
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    // Additional offset for placeables depending on how much we've collapsed
    val yOffset = lerp(statusBarHeightPx, 0, collapsedFraction)

    Layout(
        content = {
            // Nav icon
            if (onNavIconClick != null) IconButton(
                onClick = onNavIconClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (root) colorScheme.primary else colorScheme.onSurface,
                ),
                modifier = layoutIdNavIconModifier
                    .padding(horizontal = 4.dp)
                    .testTag(AppBar_IconButtonTestTag)
            ) {
                if (root) Icon(CustomIcons.LogoNotification, stringResource(R.string.about))
                else Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }

            // Title & subtitle
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = layoutIdTextModifier.padding(
                    /** Ensure it's offset even if nav icon isn't shown (i.e. when [onNavIconClick] is null) */
                    start = if (onNavIconClick != null) 0.dp else 16.dp,
                    end = 4.dp,
                )
            ) {
                Text(
                    stringResource(R.string.app_name),
                    overflow = TextOverflow.Ellipsis, maxLines = 1,
                    style = typography.titleLarge,
                    modifier = Modifier.testTag(AppBar_TitleTestTag)
                )
                Text(
                    if (subtitleResId == 0) "v${BuildConfig.VERSION_NAME}" else stringResource(subtitleResId),
                    maxLines = 1,
                    style = typography.bodyMedium,
                    modifier = Modifier
                        .basicMarquee()
                        .testTag(AppBar_SubtitleTestTag)
                )
            }

            if (actions != null) CompositionLocalProvider(
                LocalContentColor provides colorScheme.onSurfaceVariant,
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                    modifier = layoutIdActionsModifier.padding(end = 4.dp)
                )
            }
        },
        modifier = appBarModifier(scrollBehavior)
            // Leave space for 2/3-button nav bar in landscape mode
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            // Don't let content bleed outside height / inset area
            .clipToBounds()
    ) { measurables, constraints ->
        /** [LayoutIdText] measure depends on both nav & actions, so get them first */
        var navIconPlaceable: Placeable? = null
        var actionsPlaceable: Placeable? = null
        for (it in measurables) {
            when (it.layoutId) {
                LayoutIdNavIcon -> navIconPlaceable = it.measure(constraints)
                LayoutIdActions -> actionsPlaceable = it.measure(constraints)
            }
        }

        val navIconWidth = navIconPlaceable?.width ?: 0
        val actionsWidth = actionsPlaceable?.width ?: 0
        val maxWidth = constraints.maxWidth
        val textPlaceable = measurables
            .fastFirst { it.layoutId == LayoutIdText }
            .measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = (maxWidth - navIconWidth - actionsWidth).coerceAtLeast(0)
                )
            )

        layout(maxWidth, layoutHeight) {
            navIconPlaceable?.placeRelative(
                x = 0,
                y = yOffset + (layoutHeight - navIconPlaceable.height) shr 1,
            )

            textPlaceable.placeRelative(
                x = navIconWidth,
                y = yOffset + (layoutHeight - textPlaceable.height) shr 1,
            )

            actionsPlaceable?.placeRelative(
                x = maxWidth - actionsWidth,
                y = yOffset + (layoutHeight - actionsPlaceable.height) shr 1,
            )
        }
    }

    // Perf: `if (showDivider) HorizontalDivider()` causes recomposition; we avoid that by "hiding" via graphicsLayer alpha
    val dividerAlpha = if (collapsedFraction != 1f) 1f else 0f
    HorizontalDivider(Modifier.graphicsLayer { alpha = dividerAlpha })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    image: @Composable (BoxWithConstraintsScope.(Modifier) -> Unit),
    title: String,
    subtitle: String? = null,
    onNavIconClick: (() -> Unit)? = null,
) {
    val (minHeight, maxHeight) = remember { CollapsingAppBarHeight }

    val statusBarHeightPx: Int
    val heightOffset: Dp
    val heightOffsetPx = scrollBehavior.state.heightOffset
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
        if (scrollBehavior.state.heightOffsetLimit != limit) scrollBehavior.state.heightOffsetLimit = limit
    }

    // Sometimes it goes out of boundary, don't know why. Force within [0,1].
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    Layout({
        BoxWithConstraints(layoutIdImageModifier) {
            val size = Modifier.size(maxWidth, height)
            val colorScheme = MaterialTheme.colorScheme
            val hover = colorScheme.surfaceContainer.copy(alpha = 0.75f)
            val surface = colorScheme.surface
            // Lerp from 75% opacity hover to 100% opacity surface
            val background = lerp(hover, surface, collapsedFraction)

            image(size)
            Box(size.background(background))
            if (height != maxHeight) HorizontalDivider(Modifier.align(Alignment.BottomStart))
        }

        if (onNavIconClick != null) IconButton(
            onClick = onNavIconClick,
            modifier = layoutIdNavIconModifier
                .height(minHeight)
                .padding(horizontal = 4.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
        }

        Column(
            layoutIdTextModifier.padding(
                start = if (onNavIconClick != null) {
                    // Lerp only if there's a nav icon
                    lerp(20f, 56f, collapsedFraction).dp
                } else 16.dp, end = 16.dp
            )
        ) {
            CollapsingAppBarTitle(collapsedFraction = { collapsedFraction }, title = title)

            if (subtitle != null) {
                val (minSubtitleSize, maxSubtitleSize) = CollapsingAppBarSubtitleSize
                Text(
                    text = subtitle,
                    fontSize = lerp(maxSubtitleSize, minSubtitleSize, collapsedFraction).sp,
                    overflow = TextOverflow.Ellipsis, maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(AppBar_SubtitleTestTag)
                )
            }
        }
        // Note: we're intentionally not consuming horizontal system bar insets
        // because it'd be an issue only below Android 6/Marshmallow, where nav
        // bar is always black in landscape mode, even though we've setup
        // edge-to-edge properly (i.e. it should be transparent/translucent).
    }, appBarModifier(scrollBehavior).testTag(CollapsingAppBarTestTag)) { measurables, constraints ->
        var imagePlaceable: Placeable? = null
        var navIconPlaceable: Placeable? = null
        var textPlaceable: Placeable? = null
        measurables.fastForEach {
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
                it.placeRelative(0, lerp(bottom, center, accelerateInterpolator.getInterpolation(collapsedFraction)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingAppBarTitle(
    collapsedFraction: () -> Float, // must be lazy
    title: String,
) {
    // Accessibility: show full title on long press if it overflows
    var showTooltip by rememberSaveableState("showTooltip", false)
    val tooltipState = rememberTooltipState(isPersistent = true)
    BackHandler(tooltipState.isVisible) { tooltipState.dismiss() }

    val (minTitleSize, maxTitleSize) = CollapsingAppBarTitleSize
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            val colorScheme = MaterialTheme.colorScheme
            RichTooltip(
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = colorScheme.inverseSurface,
                    contentColor = colorScheme.inverseOnSurface,
                ),
                text = { Text(title) },
                modifier = Modifier.testTag(CollapsingAppBar_TooltipTestTag)
            )
        },
        state = tooltipState,
        enableUserInput = showTooltip,
        modifier = Modifier.testTag(CollapsingAppBar_TooltipBoxTestTag)
    ) {
        val fraction = collapsedFraction()
        Text(
            text = title,
            onTextLayout = { showTooltip = it.hasVisualOverflow },
            overflow = TextOverflow.Ellipsis,
            maxLines = lerp(4, 1, fraction),
            fontSize = lerp(maxTitleSize, minTitleSize, fraction).sp,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(AppBar_TitleTestTag)
        )
    }
}

private const val TAG = "AppBar"

@VisibleForTesting
const val AppBar_IconButtonTestTag = TAG + "_IconButton"

@VisibleForTesting
const val AppBar_TitleTestTag = TAG + "_Title"

@VisibleForTesting
const val AppBar_SubtitleTestTag = TAG + "_Subtitle"

@VisibleForTesting
const val CollapsingAppBarTestTag = "Collapsing$TAG"

@VisibleForTesting
const val CollapsingAppBar_ImageTestTag = CollapsingAppBarTestTag + "_Image"

@VisibleForTesting
const val CollapsingAppBar_TooltipBoxTestTag = CollapsingAppBarTestTag + "_TooltipBox"

@VisibleForTesting
const val CollapsingAppBar_TooltipTestTag = CollapsingAppBarTestTag + "_Tooltip"

private const val LayoutIdImage = "image"
private const val LayoutIdNavIcon = "navigationIcon"
private const val LayoutIdText = "text"
private const val LayoutIdActions = "actions"

private val layoutIdImageModifier = Modifier
    .layoutId(LayoutIdImage)
    .testTag(CollapsingAppBar_ImageTestTag)

private val layoutIdNavIconModifier = Modifier
    .layoutId(LayoutIdNavIcon)
    .testTag(AppBar_IconButtonTestTag)

private val layoutIdTextModifier = Modifier.layoutId(LayoutIdText)
private val layoutIdActionsModifier = Modifier.layoutId(LayoutIdActions)

/** min (material3/tokens/TopAppBarSmallTokens.ContainerHeight) to max */
@VisibleForTesting
val CollapsingAppBarHeight = 64.dp to 256.dp

/** same as LargeTitleBottomPadding in material3/AppBar.kt */
private val CollapsingAppBarBottomPadding = 28.dp

/** min (titleLarge) to max (headlineSmall) */
private val CollapsingAppBarTitleSize = 22f to 24f

/** min (bodyMedium) to max (bodyLarge) */
private val CollapsingAppBarSubtitleSize = 14f to 16f

private val accelerateInterpolator = AccelerateInterpolator(3f)

/**
 * Sets up support for resizing the top app bar when vertically dragging the bar itself
 */
@SuppressLint("ModifierFactoryExtensionFunction")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appBarModifier(
    scrollBehavior: TopAppBarScrollBehavior,
) = Modifier.draggable(
    state = rememberDraggableState { scrollBehavior.state.heightOffset += it },
    orientation = Orientation.Vertical,
    onDragStopped = {
        settleAppBar(
            state = scrollBehavior.state,
            velocity = it,
            flingAnimationSpec = scrollBehavior.flingAnimationSpec,
            snapAnimationSpec = scrollBehavior.snapAnimationSpec,
        )
    },
)

@OptIn(ExperimentalMaterial3Api::class)
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
            targetValue = if (state.collapsedFraction < 0.5f) 0f else state.heightOffsetLimit,
            animationSpec = snapAnimationSpec,
        ) { state.heightOffset = value }
    }

    return Velocity(0f, remainingVelocity)
}
