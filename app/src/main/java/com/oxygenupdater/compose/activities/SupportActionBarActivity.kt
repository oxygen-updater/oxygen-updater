package com.oxygenupdater.compose.activities

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.compose.ui.TopAppBar
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.extensions.startMainActivity

/**
 * Sets support action bar and enables home up button on the toolbar.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@OptIn(ExperimentalMaterial3Api::class)
abstract class SupportActionBarActivity(
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
) : BaseActivity() {

    protected lateinit var scrollBehavior: TopAppBarScrollBehavior

    @Composable
    protected open fun scrollBehavior() = TopAppBarDefaults.enterAlwaysScrollBehavior()

    @Composable
    protected open fun TopAppBar() = TopAppBar(scrollBehavior, {
        onBackPressed()
    }, stringResource(subtitleResId), false)

    @Composable
    protected abstract fun Content()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        setContent {
            scrollBehavior = scrollBehavior()

            AppTheme {
                EdgeToEdge()
                Scaffold(
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = { TopAppBar() },
                    // Let children handle it, so that we can achieve "overlay" navbar
                    contentWindowInsets = WindowInsets(0)
                ) {
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
