@file:Suppress("NOTHING_TO_INLINE")

package com.oxygenupdater.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.oxygenupdater.R
import com.oxygenupdater.activities.InstallActivity
import com.oxygenupdater.compose.activities.MainActivity
import com.oxygenupdater.compose.activities.MainActivity.Companion.PAGE_SETTINGS
import com.oxygenupdater.compose.activities.MainActivity.Companion.PAGE_UPDATE
import com.oxygenupdater.compose.activities.NewsItemActivity
import com.oxygenupdater.compose.activities.OnboardingActivity

inline fun <reified T : Activity> Context.startActivity() = startActivity(
    Intent(this, T::class.java)
)

inline fun Activity.startOnboardingActivity(
    @IntRange(PAGE_UPDATE.toLong(), PAGE_SETTINGS.toLong()) startPage: Int,
) = startActivity(
    Intent(this, OnboardingActivity::class.java).putExtra(
        MainActivity.INTENT_START_PAGE, startPage
    )
)

inline fun Activity.startMainActivity(
    @IntRange(PAGE_UPDATE.toLong(), PAGE_SETTINGS.toLong()) startPage: Int,
) = startActivity(
    Intent(this, MainActivity::class.java).putExtra(
        MainActivity.INTENT_START_PAGE, startPage
    )
)

inline fun Context.startInstallActivity() = startActivity(
    Intent(this, InstallActivity::class.java).putExtra(
        InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, false
    )
)

inline fun Activity.startNewsItemActivity(id: Long) = startActivity(
    Intent(this, NewsItemActivity::class.java).putExtra(
        NewsItemActivity.INTENT_NEWS_ITEM_ID, id
    )
)

/**
 * Allow activity to draw itself full screen
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Activity.enableEdgeToEdgeUiSupport() {
    if (packageManager.getActivityInfo(componentName, 0).themeResource == R.style.Theme_OxygenUpdater_DayNight) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.apply {
            doOnApplyWindowInsets { view, insets, initialPadding ->
                // initialPadding contains the original padding values after inflation
                view.updatePadding(bottom = initialPadding.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            }
        }
    }
}
