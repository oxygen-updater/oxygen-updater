package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.icons.News
import com.oxygenupdater.icons.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationBar(
    currentRoute: String?,
    navigateTo: (route: String) -> Unit,
    setSubtitleResId: (Int) -> Unit,
) = NavigationBar {
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
                label = { Text(label) },
                alwaysShowLabel = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationRail(
    currentRoute: String?,
    navigateTo: (route: String) -> Unit,
    openAboutScreen: () -> Unit,
    setSubtitleResId: (Int) -> Unit,
) = NavigationRail(header = {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
        // 64.dp => TopAppBar max height
        IconButton(openAboutScreen, Modifier.requiredHeight(64.dp)) {
            Icon(CustomIcons.LogoNotification, stringResource(R.string.about))
        }
    }
}) {
    // Allow vertical scroll in case height isn't enough for all 5 icons
    LazyColumn {
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
                label = { Text(label) },
            )
        }
    }
}

val MainScreens = arrayOf(
    Screen.Update,
    Screen.NewsList,
    Screen.Device,
    Screen.About,
    Screen.Settings,
)

@Suppress("ConvertObjectToDataObject")
@Immutable
sealed class Screen(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelResId: Int,
    val useVersionName: Boolean = false,
) {
    /** Shown only if not null (max 3 characters) */
    var badge by mutableStateOf<String?>(null)
        internal set

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
}

const val UpdateRoute = "update"
const val NewsListRoute = "newsList"
const val DeviceRoute = "device"
const val AboutRoute = "about"
const val SettingsRoute = "settings"
