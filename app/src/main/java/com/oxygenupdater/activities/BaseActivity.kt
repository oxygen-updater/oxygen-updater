package com.oxygenupdater.activities

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.oxygenupdater.extensions.attachWithLocale
import com.oxygenupdater.utils.ThemeUtils

/**
 * Single responsibility: correctly update context based on Locale preference
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class BaseActivity(
    @LayoutRes contentLayoutId: Int,
) : AppCompatActivity(contentLayoutId) {

    protected val rootView: View
        get() = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

    override fun attachBaseContext(
        base: Context,
    ) = super.attachBaseContext(base.attachWithLocale())

    override fun onResume() = super.onResume().also {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode())
    }
}
