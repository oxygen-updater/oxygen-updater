package com.oxygenupdater.compose.activities

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.oxygenupdater.extensions.attachWithLocale

/**
 * Single responsibility: correctly update context based on Locale preference
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class ComposeBaseActivity : ComponentActivity() {

    override fun attachBaseContext(base: Context) = super.attachBaseContext(base.attachWithLocale())

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        // For edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
