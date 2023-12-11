package com.oxygenupdater.ui

import android.view.animation.AccelerateInterpolator
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.util.lerp
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.theme.backgroundVariant
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onNavIconClick: () -> Unit,
    @StringRes subtitleResId: Int,
    root: Boolean,
    showIcon: Boolean = true,
    actions: @Composable (RowScope.() -> Unit)? = null,
) = Column {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val colors = TopAppBarDefaults.topAppBarColors(
        scrolledContainerColor = colorScheme.surface,
        navigationIconContentColor = if (root) colorScheme.primary else colorScheme.onSurface,
    )

    TopAppBar(
        navigationIcon = icon@{
            if (!showIcon) return@icon

            // Nav icon
            IconButton(onNavIconClick) {
                Icon(
                    if (root) CustomIcons.LogoNotification else Icons.AutoMirrored.Rounded.ArrowBack,
                    if (root) stringResource(R.string.about) else null,
                )
            }
        },
        title = {
            // Title & subtitle
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.app_name),
                    overflow = TextOverflow.Ellipsis, maxLines = 1,
                    style = typography.titleLarge,
                )
                Text(
                    if (subtitleResId == 0) "v${BuildConfig.VERSION_NAME}" else stringResource(subtitleResId),
                    maxLines = 1,
                    style = typography.bodyMedium,
                    modifier = Modifier.basicMarquee()
                )
            }
        },
        actions = { if (actions != null) actions() },
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

    // Perf: reduce recompositions by mutating only on value
    val dividerAlpha by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            if (scrollBehavior.state.collapsedFraction != 1f) 1f else 0f
        }
    }

    // Perf: `if (showDivider) ItemDivider()` causes recomposition; we avoid that by "hiding" via graphicsLayer alpha
    ItemDivider(Modifier.graphicsLayer { alpha = dividerAlpha })
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

    // Set up support for resizing the top app bar when vertically dragging the bar itself
    val appBarModifier = Modifier.draggable(
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

    // Sometimes it goes out of boundary, don't know why. Force within [0,1].
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    Layout({
        BoxWithConstraints(layoutIdImageModifier) {
            val size = Modifier.size(maxWidth, height)
            val colorScheme = MaterialTheme.colorScheme
            val hover = colorScheme.backgroundVariant.copy(alpha = 0.75f)
            val surface = colorScheme.surface
            // Lerp from 75% opacity hover to 100% opacity surface
            val background = lerp(hover, surface, collapsedFraction)

            image(size)
            Box(size.background(background))
            if (height != maxHeight) ItemDivider(Modifier.align(Alignment.BottomStart))
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
            layoutIdTextModifier
                .padding(
                    start = if (onNavIconClick != null) {
                        // Lerp only if there's a nav icon
                        lerp(20f, 56f, collapsedFraction).dp
                    } else 16.dp, end = 16.dp
                )
        ) {
            CollapsingAppBarTitle(scrollBehavior = scrollBehavior, title = title)

            if (subtitle != null) {
                val (minSubtitleSize, maxSubtitleSize) = CollapsingAppBarSubtitleSize
                Text(
                    text = subtitle,
                    fontSize = lerp(maxSubtitleSize, minSubtitleSize, collapsedFraction).sp,
                    overflow = TextOverflow.Ellipsis, maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
                it.placeRelative(0, lerp(bottom, center, accelerateInterpolator.getInterpolation(collapsedFraction)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingAppBarTitle(scrollBehavior: TopAppBarScrollBehavior, title: String) {
    // Accessibility: show full title on long press if it overflows
    var showTooltip by rememberSaveableState("showTooltip", false)
    val tooltipState = rememberTooltipState(isPersistent = true)
    BackHandler(tooltipState.isVisible) { tooltipState.dismiss() }

    val (minTitleSize, maxTitleSize) = CollapsingAppBarTitleSize
    // Sometimes it goes out of boundary, don't know why. Force within [0,1].
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    TooltipBox(TooltipDefaults.rememberRichTooltipPositionProvider(), {
        val colorScheme = MaterialTheme.colorScheme
        RichTooltip(
            colors = TooltipDefaults.richTooltipColors(
                containerColor = colorScheme.inverseSurface,
                contentColor = colorScheme.inverseOnSurface,
            ),
            text = { Text(title) },
        )
    }, tooltipState, enableUserInput = showTooltip) {
        Text(
            text = title,
            onTextLayout = { showTooltip = it.hasVisualOverflow },
            overflow = TextOverflow.Ellipsis,
            maxLines = lerp(4, 1, collapsedFraction),
            fontSize = lerp(maxTitleSize, minTitleSize, collapsedFraction).sp,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

private const val LayoutIdImage = "image"
private const val LayoutIdNavIcon = "navigationIcon"
private const val LayoutIdText = "text"

private val layoutIdImageModifier = Modifier.layoutId(LayoutIdImage)
private val layoutIdNavIconModifier = Modifier.layoutId(LayoutIdNavIcon)
private val layoutIdTextModifier = Modifier.layoutId(LayoutIdText)

/** min (material3/tokens/TopAppBarSmallTokens.ContainerHeight) to max */
private val CollapsingAppBarHeight = 64.dp to 256.dp

/** same as LargeTitleBottomPadding in material3/AppBar.kt */
private val CollapsingAppBarBottomPadding = 28.dp

/** min (titleLarge) to max (headlineSmall) */
private val CollapsingAppBarTitleSize = 22f to 24f

/** min (bodyMedium) to max (bodyLarge) */
private val CollapsingAppBarSubtitleSize = 14f to 16f

private val accelerateInterpolator = AccelerateInterpolator(3f)

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
