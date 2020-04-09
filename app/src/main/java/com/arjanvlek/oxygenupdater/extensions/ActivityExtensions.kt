package com.arjanvlek.oxygenupdater.extensions

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.updatePadding
import com.arjanvlek.oxygenupdater.R
import com.google.android.material.appbar.AppBarLayout

/**
 * Allow activity to draw itself full screen
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Activity.enableEdgeToEdgeUiSupport() {
    if (packageManager.getActivityInfo(componentName, 0).themeResource == R.style.Theme_Oxygen_FullScreen) {
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

            doOnApplyWindowInsets { view, insets, initialPadding ->
                // initialPadding contains the original padding values after inflation
                view.updatePadding(bottom = initialPadding.bottom + insets.systemWindowInsetBottom)
            }
        }
    }
}

fun Activity.makeSceneTransitionAnimationBundle() = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *createTransitionPairs().toTypedArray()).toBundle()

/**
 * Creates transition pairs for status bar, navigation bar, and app bar
 */
fun Activity.createTransitionPairs(): List<Pair<View, String>> = arrayListOf<Pair<View, String>>().apply {
    val appBarLayout = findViewById<AppBarLayout>(R.id.appBar)
    val statusBar = findViewById<View>(android.R.id.statusBarBackground)
    val navigationBar = findViewById<View>(android.R.id.navigationBarBackground)

    if (statusBar != null) {
        add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
    }

    if (navigationBar != null) {
        add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
    }

    if (appBarLayout != null) {
        add(Pair.create(appBarLayout, "toolbar"))
    }
}
