package com.oxygenupdater.compose.activities

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.TopAppBar
import com.oxygenupdater.compose.ui.TopAppBarDefaults
import com.oxygenupdater.compose.ui.TopAppBarScrollBehavior
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.compose.ui.theme.light
import com.oxygenupdater.extensions.startMainActivity

/**
 * Sets support action bar and enables home up button on the toolbar.
 * Additionally, it sets up:
 * * shared element transitions (see [MaterialContainerTransform])
 * * a full screen activity if its theme is [R.style.Theme_OxygenUpdater_DayNight_FullScreen]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class ComposeSupportActionBarActivity(
    /**
     * Used in the `onBackPressed` callback, only if this is the task root and
     * we need to reroute to [MainActivity].
     *
     * Certain activities (e.g. install, news) can take advantage of this to
     * tie back to the correct tab, if opened from a notification for example.
     */
    @IntRange(MainActivity.PAGE_UPDATE.toLong(), MainActivity.PAGE_SETTINGS.toLong())
    private val startPage: Int,

    @StringRes private val subtitleResId: Int,
) : ComposeBaseActivity() {

    protected lateinit var scrollBehavior: TopAppBarScrollBehavior

    @Composable
    protected open fun scrollBehavior() = TopAppBarDefaults.enterAlwaysScrollBehavior()

    @Composable
    protected open fun SystemBars() {
        val colors = MaterialTheme.colors
        val controller = rememberSystemUiController()
        val darkIcons = colors.light
        controller.setNavigationBarColor(Color.Transparent, darkIcons)
        controller.setStatusBarColor(colors.surface, darkIcons)
    }

    @Composable
    protected open fun TopAppBar() = TopAppBar(scrollBehavior, {
        onBackPressed()
    }, stringResource(subtitleResId), Modifier.statusBarsPadding(), false)

    @Composable
    protected abstract fun Content()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        setContent {
            scrollBehavior = scrollBehavior()

            AppTheme {
                SystemBars()

                Scaffold(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = { TopAppBar() }) {
                    Box(Modifier.padding(it)) {
                        Content()
                    }
                }
            }

            BackHandler {
                /** If this is the only activity left in the stack, launch [MainActivity] */
                if (isTaskRoot) startMainActivity(startPage) else finishAfterTransition()
            }
        }
    }
}
