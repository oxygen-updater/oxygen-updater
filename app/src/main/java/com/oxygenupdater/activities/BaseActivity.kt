package com.oxygenupdater.activities

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.oxygenupdater.extensions.attachWithLocale

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
}
