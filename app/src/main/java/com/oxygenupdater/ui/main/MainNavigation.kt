package com.oxygenupdater.ui.main

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.unveilIn
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.oxygenupdater.R
import com.oxygenupdater.icons.ArrowBack
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.theme.light

@Composable
fun MainNavigationBar(
    currentRoute: Route?,
    navigateTo: (route: MainRoute) -> Unit,
) = NavigationBar(Modifier.testTag(MainNavigation_BarTestTag)) {
    MainRoutes.forEach { screen ->
        val labelResId = screen.labelResId
        key(labelResId) { // because this is in a `forEach`
            val label = stringResource(labelResId)
            val selected = currentRoute === screen
            NavigationBarItem(
                selected = selected,
                onClick = { navigateTo(screen) },
                icon = {
                    val badge = screen.badge
                    if (badge == null) Icon(screen.icon, label) else BadgedBox({
                        Badge {
                            Text(badge.take(3), Modifier.semantics {
                                contentDescription = "$badge unread articles"
                            })
                        }
                    }) { Icon(screen.icon, label) }
                },
                label = { NavigationLabel(label = label) },
                alwaysShowLabel = false,
            )
        }
    }
}

@Composable
fun MainNavigationRail(
    currentRoute: Route?,
    root: Boolean,
    onNavIconClick: () -> Unit,
    navigateTo: (route: MainRoute) -> Unit,
) = NavigationRail(
    header = {
        val colorScheme = MaterialTheme.colorScheme
        val color = if (root) colorScheme.primary else colorScheme.onSurface
        CompositionLocalProvider(LocalContentColor provides color) {
            // 64.dp => TopAppBar max height
            IconButton(
                onClick = onNavIconClick,
                modifier = Modifier
                    .requiredHeight(64.dp)
                    .testTag(MainNavigation_Rail_IconButtonTestTag)
            ) {
                Icon(
                    if (root) Logos.LogoNotification else Symbols.ArrowBack,
                    if (root) stringResource(R.string.about) else null,
                )
            }
        }
    },
    modifier = Modifier.testTag(MainNavigation_RailTestTag)
) {
    // Allow vertical scroll in case height isn't enough for all 5 icons
    LazyColumn(Modifier.testTag(MainNavigation_Rail_LazyColumnTestTag)) {
        items(MainRoutes) { screen ->
            val labelResId = screen.labelResId
            val label = stringResource(labelResId)
            val selected = currentRoute === screen
            NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(screen) },
                icon = {
                    val badge = screen.badge
                    if (badge == null) Icon(screen.icon, label) else BadgedBox({
                        Badge {
                            Text(badge.take(3), Modifier.semantics {
                                contentDescription = "$badge unread articles"
                            })
                        }
                    }) { Icon(screen.icon, label) }
                },
                label = { NavigationLabel(label = label) },
            )
        }
    }
}

@Composable
private inline fun NavigationLabel(label: String) = Text(
    text = label,
    maxLines = 1,
    modifier = Modifier
        .basicMarquee()
        .testTag(MainNavigation_LabelTestTag)
)

typealias TransitionSpecReceiver<T> = AnimatedContentTransitionScope<Scene<T>>
typealias TransitionSpecFn = TransitionSpecReceiver<Route>.() -> ContentTransform
typealias PredictiveTransitionSpecFn = TransitionSpecReceiver<Route>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform

/**
 * Simple transition to convey the back stack to the user: when popping, both the outgoing and
 * incoming screens scale down while simultaneously fading in/out respectively.
 *
 * The outgoing screen scales down to 0.7x its original size, and the incoming screen scales down
 * from 1.1x its final size.
 *
 * Should be paired with [scaleChildTransitionSpec] for child routes.
 */
fun scalePopTransitionSpec(): TransitionSpecFn = {
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 1.1f) + fadeIn(),
        initialContentExit = scaleOut(targetScale = 0.7f) + fadeOut(),
    )
}

fun directionalScalePredictivePopTransitionSpec(): PredictiveTransitionSpecFn = { edge ->
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 1.1f) + fadeIn(),
        initialContentExit = scaleOut(
            targetScale = 0.7f, transformOrigin = when (edge) {
                NavigationEvent.EDGE_LEFT -> TransformOrigin(1f, 0.5f)
                NavigationEvent.EDGE_RIGHT -> TransformOrigin(0f, 0.5f)
                else -> TransformOrigin.Center
            }
        ) + fadeOut(),
    )
}

/**
 * Simple transition to convey the back stack to the user: when navigating, both the outgoing and
 * incoming screens scale up while simultaneously fading in/out respectively.
 *
 * The outgoing screen scales up to 1.1x its original size, and the incoming screen scales up
 * from 0.7x its final size.
 *
 * Should be paired with [scalePopTransitionSpec] for the pop transition.
 */
fun scaleChildTransitionSpec(): TransitionSpecReceiver<*>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 0.7f) + fadeIn(),
        initialContentExit = scaleOut(targetScale = 1.1f) + fadeOut(),
    )
}

/**
 * Kept in case I decide to change the [androidx.navigation3.ui.NavDisplay] transitions later.
 * This mimics the M3-recommended forward/backward transition: slide horizontally while fading
 * in/out respectively.
 */
fun slidePopTransitionSpec(): TransitionSpecFn = {
    // Half the duration of predictive pop
    val durationMillis = AnimationConstants.DefaultDurationMillis / 2
    ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearOutSlowInEasing,
            ),
            // Start off slightly off the screen
            initialOffsetX = { -it / 4 },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing, // match exit
            ),
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing,
            ),
            // Reduce visual load by moving the content only a little bit,
            // relying on fade out for the rest.
            targetOffsetX = { it / 4 },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing,
            ),
        ),
    )
}

/**
 * Kept in case I decide to change the [androidx.navigation3.ui.NavDisplay] transitions later.
 * This mimics the M3-recommended forward/backward transition: slide horizontally while fading
 * in/out respectively.
 */
fun scrimmedContainerSlidePredictivePopTransitionSpec(colorScheme: ColorScheme): PredictiveTransitionSpecFn = {
    // We always enter from the left and exit from the right, regardless of swipe edge.
    // This mimics Android's animation between 2 apps.
    @OptIn(ExperimentalAnimationApi::class)
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(easing = LinearOutSlowInEasing),
            // Start off slightly off the screen
            initialOffset = { -it / 4 },
        ) + unveilIn(
            // scrim overlay to indicate it's 'below' the exiting screen
            animationSpec = tween(easing = FastOutLinearInEasing), // match exit
            initialColor = colorScheme.run {
                // Use a dark veil on both themes. Surface colours are
                // preferred over Black (or scrim) to maintain theme colours.
                if (light) inverseSurface.copy(alpha = 0.7f) else surface
            },
        ),
        initialContentExit = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(easing = FastOutLinearInEasing),
            targetOffset = { it },
        ),
    )
}


private const val TAG = "MainNavigation"

@VisibleForTesting
const val MainNavigation_BarTestTag = TAG + "_Bar"

@VisibleForTesting
const val MainNavigation_RailTestTag = TAG + "_Rail"

@VisibleForTesting
const val MainNavigation_LabelTestTag = TAG + "_Label"

@VisibleForTesting
const val MainNavigation_Rail_LazyColumnTestTag = TAG + "_Rail_LazyColumn"

@VisibleForTesting
const val MainNavigation_Rail_IconButtonTestTag = TAG + "_Rail_IconButton"
