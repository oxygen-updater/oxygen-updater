package com.oxygenupdater.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.ThemeUtils
import org.koin.java.KoinJavaComponent.getKoin

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
    sizeBytes: Long
): String = Formatter.formatFileSize(
    this,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (sizeBytes / 1.048576).toLong()
    } else {
        sizeBytes
    }
)

fun Context.openPlayStorePage() {
    val appPackageName = packageName

    try {
        // Try opening Play Store
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$appPackageName")
            ).withAppReferrer(this)
        )
    } catch (e: ActivityNotFoundException) {
        try {
            // Try opening browser
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                ).withAppReferrer(this)
            )
        } catch (e1: ActivityNotFoundException) {
            // Give up and cry
            Toast.makeText(
                this,
                getString(R.string.error_unable_to_rate_app),
                Toast.LENGTH_LONG
            ).show()
            Logger.logWarning("AboutActivity", "App rating without google play store support", e1)
        }
    }
}

fun Context.openEmail() {
    val systemVersionProperties by getKoin().inject<SystemVersionProperties>()
    val oxygenOsVersion = systemVersionProperties.oxygenOSVersion
    val oxygenOsOtaVersion = systemVersionProperties.oxygenOSOTAVersion
    val osType = systemVersionProperties.osType
    val actualDeviceName = systemVersionProperties.oxygenDeviceName
    val appVersion = BuildConfig.VERSION_NAME
    val chosenDeviceName = SettingsManager.getPreference(
        SettingsManager.PROPERTY_DEVICE,
        "<UNKNOWN>"
    )
    val chosenUpdateMethod = SettingsManager.getPreference(
        SettingsManager.PROPERTY_UPDATE_METHOD,
        "<UNKNOWN>"
    )
    val advancedModeEnabled = SettingsManager.getPreference(
        SettingsManager.PROPERTY_ADVANCED_MODE,
        false
    )

    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SENDTO)
                .setData(Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email_address)))
                // Parts of this should probably be localized but it may pose a
                // problem for us while reading emails
                .putExtra(
                    Intent.EXTRA_TEXT,
                    """
                        --------------------
                        • Actual device: $actualDeviceName
                        • Chosen device: $chosenDeviceName
                        • Update method: $chosenUpdateMethod
                        • OS version: $oxygenOsVersion ($osType)
                        • OTA version: $oxygenOsOtaVersion
                        • Advanced mode: $advancedModeEnabled
                        • App version: $appVersion
                        --------------------
                        
                        <write your query here>
                    """.trimIndent()
                ),
            getString(R.string.about_email_button_text)
        )
    )
}

fun Context.openDiscord() = startActivity(
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse(getString(R.string.discord_url))
    ).withAppReferrer(this)
)

fun Context.openGitHub() = startActivity(
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse(getString(R.string.github_url))
    ).withAppReferrer(this)
)

fun Context.openPatreon() = startActivity(
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse(getString(R.string.patreon_url))
    ).withAppReferrer(this)
)

fun Context.openWebsite() = startActivity(
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse(getString(R.string.website_url))
    ).withAppReferrer(this)
)

fun Context.openAppDetailsPage() {
    val action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    try {
        startActivity(Intent(action, Uri.parse("package:$packageName")))
    } catch (e: Exception) {
        logError("ContextExtensions", "openAppDetailsPage failed", e)
    }
}

fun Context.openInCustomTab(url: String) = customTabIntent().launchUrl(
    this,
    Uri.parse(url)
)

private fun Context.customTabIntent() = CustomTabsIntent.Builder()
    .setShowTitle(true)
    .setUrlBarHidingEnabled(true)
    .setDefaultColorSchemeParams(
        CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(this, R.color.background))
            .setNavigationBarColor(ContextCompat.getColor(this, R.color.background))
            .build()
    )
    .setColorScheme(
        if (ThemeUtils.isNightModeActive(this)) {
            CustomTabsIntent.COLOR_SCHEME_DARK
        } else {
            CustomTabsIntent.COLOR_SCHEME_LIGHT
        }
    ).build().apply {
        intent.withAppReferrer(this@customTabIntent)
    }
