package com.oxygenupdater.activities

import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.oxygenupdater.ui.theme.light

/**
 * Single responsibility: enable edge-to-edge.
 *
 * We're using [AppCompatActivity] instead of [androidx.activity.ComponentActivity] because of
 * [automatic per-app language](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class BaseActivity : AppCompatActivity() {

    @Composable
    @ReadOnlyComposable
    protected fun EdgeToEdge() {
        val light = MaterialTheme.colorScheme.light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { !light },
            navigationBarStyle = navigationBarStyle,
        )
    }

    companion object {
        /**
         * Force even 3-button nav to be completely transparent on [Android 10+](https://github.com/android/nowinandroid/pull/817#issuecomment-1647079628)
         */
        private val navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    }
}
