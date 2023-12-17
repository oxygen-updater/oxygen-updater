package com.oxygenupdater.activities

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.ui.TopAppBar
import com.oxygenupdater.ui.theme.AppTheme

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
    @IntRange(MainActivity.PageUpdate.toLong(), MainActivity.PageSettings.toLong())
    private val startPage: Int,

    @StringRes private val subtitleResId: Int,
) : BaseActivity() {

    protected lateinit var scrollBehavior: TopAppBarScrollBehavior

    @Composable
    protected open fun scrollBehavior() = TopAppBarDefaults.enterAlwaysScrollBehavior()

    @Composable
    protected open fun TopAppBar() = TopAppBar(
        scrollBehavior = scrollBehavior,
        navIconClicked = {
            onBackPressedDispatcher.onBackPressed()
        },
        subtitleResId = subtitleResId,
        root = false,
    )

    @Composable
    protected abstract fun Content(modifier: Modifier)

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        setContent {
            scrollBehavior = scrollBehavior()

            AppTheme {
                EdgeToEdge()
                // We're using Surface to avoid Scaffold's recomposition-on-scroll issue (when using scrollBehaviour and consuming innerPadding)
                Surface {
                    Column {
                        TopAppBar()
                        Content(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection))
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
