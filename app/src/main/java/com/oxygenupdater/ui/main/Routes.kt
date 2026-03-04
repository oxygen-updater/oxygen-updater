package com.oxygenupdater.ui.main

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.oxygenupdater.R
import com.oxygenupdater.icons.Help
import com.oxygenupdater.icons.Mobile
import com.oxygenupdater.icons.Newsmode
import com.oxygenupdater.icons.Settings
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.icons.SystemUpdateAlt
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    val labelResId: Int // StringRes

    /**
     * Required, must be unique. Used as a key in [androidx.navigation3.runtime.EntryProviderScope.entry]
     * via [androidx.navigation3.runtime.defaultContentKey].
     */
    override fun toString(): String
}

sealed interface ChildRoute : Route

val MainRoutes = arrayOf(
    MainRoute.Update,
    MainRoute.NewsList,
    MainRoute.Device,
    MainRoute.About,
    MainRoute.Settings,
)

@Immutable
sealed class MainRoute(
    val icon: ImageVector,
    @StringRes override val labelResId: Int,
    val useVersionName: Boolean = false,
) : Route {

    /** Shown only if not null (max 3 characters) */
    var badge by mutableStateOf<String?>(null)

    @Stable
    object Update : MainRoute(Symbols.SystemUpdateAlt, R.string.update_information_header) {
        override fun toString() = "MainRoute.Update"
    }

    @Stable
    object NewsList : MainRoute(Symbols.Newsmode, R.string.news) {
        override fun toString() = "MainRoute.NewsList"
    }

    @Stable
    object Device : MainRoute(Symbols.Mobile, R.string.device_information_header) {
        override fun toString() = "MainRoute.Device"
    }

    @Stable
    object About : MainRoute(Symbols.Help, R.string.about, true) {
        override fun toString() = "MainRoute.About"
    }

    @Stable
    object Settings : MainRoute(Symbols.Settings, R.string.settings) {
        override fun toString() = "MainRoute.Settings"
    }
}

@Serializable
@Immutable
data class ArticleRoute(
    /** Required */
    val id: Long,

    /**
     * Optional, should only be set when opening via notification
     *
     * @see com.oxygenupdater.workers.DisplayDelayedNotificationWorker.getNotificationIntent
     */
    val external: Boolean = false,
) : ChildRoute {

    @StringRes
    override val labelResId = R.string.news

    override fun toString() = "ChildScreen.Article"
}

@Serializable
@Immutable
data class GuideRoute(
    /** Optional, defaults to `false` */
    val downloaded: Boolean = false,
) : ChildRoute {

    @StringRes
    override val labelResId = R.string.install_guide

    override fun toString() = "ChildScreen.Guide"
}

@Serializable
@Immutable
object FaqRoute : ChildRoute {

    @StringRes
    override val labelResId = R.string.faq

    override fun toString() = "ChildScreen.Faq"
}

const val OuScheme = "oxygenupdater"
const val OuSchemeSuffixed = "$OuScheme://"

const val ExternalArg = "external"
const val DownloadedArg = "downloaded"

const val DeepLinkArticle = "article"
const val DeepLinkGuide = "guide"
const val DeepLinkFaq = "faq"

const val DeepLinkUpdate = "update"
const val DeepLinkNewsList = "news"
const val DeepLinkDevice = "device"
const val DeepLinkAbout = "about"
const val DeepLinkSettings = "settings"
