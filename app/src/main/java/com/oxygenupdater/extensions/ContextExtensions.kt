package com.oxygenupdater.extensions

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.theme.light
import com.oxygenupdater.utils.logError
import com.oxygenupdater.utils.logWarning

/**
 * Standardizes display of file sizes across the app, regardless of OS versions.
 *
 * Wraps around [Formatter.formatFileSize], which formats differently on different
 * API levels. Pre-Oreo (API level 26), the IEC format is used (i.e. mebibyte
 * instead of megabyte), while SI units are used on Oreo and above.
 *
 * Normally in the context of files & downloading, IEC formats are used, which our
 * admin portal of course follows. File managers also report sizes in IEC, as do
 * download managers. OnePlus reports in IEC on their website and their OTA screens,
 * but the "Local upgrade" screen uses SI units for some reason (yay inconsistency).
 *
 * The IEC vs SI debate is an annoying & long-standing one, but so far it looks like
 * most software products report in IEC. So it makes sense to guarantee consistency
 * not just with values reported by our admin portal, but also by OnePlus (in most
 * places) and any file manager the user might have installed.
 *
 * Note: 1 MiB =  1048576 bytes (IEC), while 1 MB = 1000000 bytes (SI)
 *
 * @param sizeBytes the size, in bytes
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Context.formatFileSize(
    sizeBytes: Long,
): String = Formatter.formatFileSize(
    this,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) (sizeBytes / 1.048576).toLong() else sizeBytes
)

fun Context.showToast(
    @StringRes resId: Int,
    duration: Int = Toast.LENGTH_LONG,
) = Toast.makeText(this, resId, duration).show()

fun Context.showToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

fun Context.shareExternally(title: String, text: String) {
    val prefix = "${getString(R.string.app_name)}: $title"
    val intent = Intent(Intent.ACTION_SEND)
        // Rich text preview: should work only on API 29+
        .putExtra(Intent.EXTRA_TITLE, title)
        // Main share content: will work on all API levels
        .putExtra(Intent.EXTRA_TEXT, "$prefix\n\n$text")
        .setType("text/plain")

    startActivity(Intent.createChooser(intent, null))
}

@SuppressLint("PrivateResource")
fun Context.copyToClipboard(text: String) = getSystemService<ClipboardManager>()?.setPrimaryClip(
    ClipData.newPlainText(getString(R.string.app_name), text)
)?.let {
    showToast(androidx.browser.R.string.copy_toast_msg)
}

fun Context.openPlayStorePage() {
    val packageName = packageName
    val url = "https://play.google.com/store/apps/details?id=$packageName"
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).withAppReferrer(packageName)

    try {
        // Try opening Play Store: https://developer.android.com/distribute/marketing-tools/linking-to-google-play#android-app
        startActivity(intent.setPackage("com.android.vending"))
    } catch (e: ActivityNotFoundException) {
        try {
            // Try opening browser
            startActivity(intent)
        } catch (e1: ActivityNotFoundException) {
            logWarning("ContextExtensions", "Can't open Play Store app page", e1)
            showToast(R.string.error_unable_to_rate_app, Toast.LENGTH_SHORT)
            // Fallback: copy to clipboard instead
            copyToClipboard(url)
        }
    }
}

fun Context.openEmail() {
    val chosenDevice = PrefManager.getString(PrefManager.KeyDevice, "<UNKNOWN>")
    val chosenMethod = PrefManager.getString(PrefManager.KeyUpdateMethod, "<UNKNOWN>")
    val advancedMode = PrefManager.getBoolean(PrefManager.KeyAdvancedMode, false)
    val osVersionWithType = SystemVersionProperties.oxygenOSVersion + SystemVersionProperties.osType.let {
        if (it.isNotEmpty()) " ($it)" else ""
    }

    // Don't localize any part of this, it'll be an annoyance for us while reading emails
    val emailBody = """
--------------------
• Device: $chosenDevice (${SystemVersionProperties.oxygenDeviceName})
• Method: $chosenMethod
• OS version: $osVersionWithType
• OTA version: ${SystemVersionProperties.oxygenOSOTAVersion}
• Advanced mode: $advancedMode
• App version: ${BuildConfig.VERSION_NAME}
--------------------

<write your query here>"""

    try {
        startActivity(
            Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                .putExtra(Intent.EXTRA_EMAIL, arrayOf("support@oxygenupdater.com"))
                .putExtra(Intent.EXTRA_TEXT, emailBody)
        )
    } catch (e: ActivityNotFoundException) {
        // TODO(translate)
        showToast("You don't appear to have an email client installed on your phone")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Context.openAppDetailsPage() = try {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageNameUri))
} catch (e: Exception) {
    logError("ContextExtensions", "openAppDetailsPage failed", e)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("NOTHING_TO_INLINE")
@Throws(ActivityNotFoundException::class)
inline fun Context.openAppLocalePage() = startActivity(
    Intent(Settings.ACTION_APP_LOCALE_SETTINGS, packageNameUri)
)

inline val Context.packageNameUri
    get() = "package:$packageName".toUri()

fun CustomTabsIntent.launch(context: Context, url: String) = apply {
    intent.withAppReferrer(context.packageName)
}.launchUrl(context, url.toUri())

@Composable
fun rememberCustomTabsIntent(): CustomTabsIntent {
    val colorScheme = MaterialTheme.colorScheme
    val surface = colorScheme.surface
    val light = colorScheme.light
    return remember(surface, light) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(surface.toArgb().let {
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(it)
                    .setNavigationBarColor(it)
                    .build()
            }).setColorScheme(
                if (light) CustomTabsIntent.COLOR_SCHEME_DARK
                else CustomTabsIntent.COLOR_SCHEME_LIGHT
            ).build()
    }
}
