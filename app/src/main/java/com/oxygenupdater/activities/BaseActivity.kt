package com.oxygenupdater.activities

import android.content.Context
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
    @LayoutRes contentLayoutId: Int
) : AppCompatActivity(contentLayoutId) {

    override fun attachBaseContext(
        base: Context
    ) = super.attachBaseContext(base.attachWithLocale())

    override fun onResume() = super.onResume().also {
        AppCompatDelegate.setDefaultNightMode(
            ThemeUtils.translateThemeToNightMode(this)
        )
    }
}
