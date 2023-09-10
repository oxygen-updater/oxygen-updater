package com.oxygenupdater.ui.theme

import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.getSystemService
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.preferencesModule
import com.oxygenupdater.ui.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.Calendar

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

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
            remember {
                Calendar.getInstance()[Calendar.HOUR_OF_DAY].let { hour ->
                    if (hour in 19..23 || hour in 0..6) true
                    else context.getSystemService<PowerManager>()?.isPowerSaveMode == true
                }
            }
        }
    }

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme, typography = remember { appTypography() }, content = content)
}

/**
 * TODO(compose): this function only serves to initialize Koin with [PrefManager],
 * so that we can use state variables like [PrefManager.theme].
 */
@Composable
fun PreviewAppTheme(content: @Composable () -> Unit) {
    if (GlobalContext.getOrNull() == null) {
        val context = LocalContext.current
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(context)
            modules(preferencesModule)
        }
    }

    AppTheme(content = { Surface(content = content) })
}

@Preview("Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview("Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PreviewThemes
