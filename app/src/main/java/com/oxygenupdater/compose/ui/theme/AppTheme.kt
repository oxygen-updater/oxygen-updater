package com.oxygenupdater.compose.ui.theme

import android.content.res.Configuration
import android.os.PowerManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.getSystemService
import com.oxygenupdater.compose.ui.Theme
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.preferencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Composable
@NonRestartableComposable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(colors, appTypography(), content = content)

/**
 * TODO(compose): this function only serves to initialize Koin with [PrefManager],
 * so that we can use state variables like [PrefManager.theme].
 */
@Composable
@NonRestartableComposable
fun PreviewAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    if (GlobalContext.getOrNull() == null) startKoin {
        androidLogger(Level.ERROR)
        androidContext(context)
        modules(preferencesModule)
    }

    MaterialTheme(colors, appTypography(), content = { Surface(content = content) })
}

private inline val colors: Colors
    @Composable
    @NonRestartableComposable
    get() {
        val theme by remember { derivedStateOf(structuralEqualityPolicy()) { PrefManager.theme } }

        val dark = remember(theme) {
            when (theme) {
                Theme.Light -> false
                Theme.Dark -> true
                else -> null
            }
        }.let {
            if (it != null) return@let it

            if (theme == Theme.System) isSystemInDarkTheme() else {
                // Avoid a potentially expensive call
                val context = LocalContext.current
                remember {
                    context.getSystemService<PowerManager>()?.isPowerSaveMode == true
                }
            }
        }

        return if (dark) DarkColors else LightColors
    }

@Preview("Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview("Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PreviewThemes
