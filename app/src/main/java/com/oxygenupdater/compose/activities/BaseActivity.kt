package com.oxygenupdater.compose.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Single responsibility: enable edge-to-edge.
 *
 * We're using [AppCompatActivity] instead of [androidx.activity.ComponentActivity] because of
 * [automatic per-app language](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        // For edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
