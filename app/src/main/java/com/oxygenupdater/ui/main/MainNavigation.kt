package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.icons.News
import com.oxygenupdater.icons.Settings

@Composable
fun MainNavigationBar(
    currentRoute: String?,
    navigateTo: (route: String) -> Unit,
    setSubtitleResId: (Int) -> Unit,
) = NavigationBar(Modifier.testTag(MainNavigation_BarTestTag)) {
    MainScreens.forEach { screen ->
        val labelResId = screen.labelResId
        key(labelResId) { // because this is in a `forEach`
            val route = screen.route
            val label = stringResource(labelResId)
            val selected = currentRoute == route
            if (selected) {
                // UpdateScreen manages its own subtitle, so avoid race by not setting at all
                // 0 => set to app version
                if (labelResId != Screen.Update.labelResId) setSubtitleResId(
                    if (screen.useVersionName) 0 else labelResId
                )
            }
            NavigationBarItem(
                selected = selected,
                onClick = { navigateTo(route) },
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
    currentRoute: String?,
    root: Boolean,
    onNavIconClick: () -> Unit,
    navigateTo: (route: String) -> Unit,
    setSubtitleResId: (Int) -> Unit,
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
                    if (root) CustomIcons.LogoNotification else Icons.AutoMirrored.Rounded.ArrowBack,
                    if (root) stringResource(R.string.about) else null,
                )
            }
        }
    },
    modifier = Modifier.testTag(MainNavigation_RailTestTag)
) {
    // Allow vertical scroll in case height isn't enough for all 5 icons
    LazyColumn(Modifier.testTag(MainNavigation_Rail_LazyColumnTestTag)) {
        items(MainScreens) { screen ->
            val labelResId = screen.labelResId
            val route = screen.route
            val label = stringResource(labelResId)
            val selected = currentRoute == route
            if (selected) {
                // UpdateScreen manages its own subtitle, so avoid race by not setting at all
                // 0 => set to app version
                if (labelResId != Screen.Update.labelResId) setSubtitleResId(
                    if (screen.useVersionName) 0 else labelResId
                )
            }
            NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(route) },
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

@OptIn(ExperimentalFoundationApi::class)
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun NavigationLabel(label: String) = Text(
    text = label,
    maxLines = 1,
    modifier = Modifier
        .basicMarquee()
        .testTag(MainNavigation_LabelTestTag)
)

val MainScreens = arrayOf(
    Screen.Update,
    Screen.NewsList,
    Screen.Device,
    Screen.About,
    Screen.Settings,
)

@Immutable
sealed class Screen(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelResId: Int,
    val useVersionName: Boolean = false,
) {
    /** Shown only if not null (max 3 characters) */
    var badge by mutableStateOf<String?>(null)

    @Stable
    object Update : Screen(UpdateRoute, Icons.Rounded.SystemUpdateAlt, R.string.update_information_header)

    @Stable
    object NewsList : Screen(NewsListRoute, CustomIcons.News, R.string.news)

    @Stable
    object Device : Screen(DeviceRoute, Icons.Rounded.PhoneAndroid, R.string.device_information_header)

    @Stable
    object About : Screen(AboutRoute, Icons.AutoMirrored.Rounded.HelpOutline, R.string.about, true)

    @Stable
    object Settings : Screen(SettingsRoute, CustomIcons.Settings, R.string.settings)

    override fun toString() = "Screen.$route"
}

const val UpdateRoute = "update"
const val NewsListRoute = "news"
const val DeviceRoute = "device"
const val AboutRoute = "about"
const val SettingsRoute = "settings"

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
