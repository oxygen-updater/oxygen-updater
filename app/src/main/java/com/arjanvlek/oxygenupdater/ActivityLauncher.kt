package com.arjanvlek.oxygenupdater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.arjanvlek.oxygenupdater.about.AboutActivity
import com.arjanvlek.oxygenupdater.contribution.ContributorActivity
import com.arjanvlek.oxygenupdater.faq.FAQActivity
import com.arjanvlek.oxygenupdater.help.HelpActivity
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.settings.SettingsActivity
import com.arjanvlek.oxygenupdater.setupwizard.SetupActivity
import com.google.android.material.appbar.AppBarLayout
import java.lang.ref.WeakReference

@Suppress("Unused", "FunctionName")
class ActivityLauncher(baseActivity: Activity) {

    private val baseActivity: WeakReference<Activity> = WeakReference(baseActivity)

    fun openPlayStorePage(context: Context) {
        val appPackageName = context.packageName

        try {
            // try opening Play Store
            context.startActivity(Intent(ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: ActivityNotFoundException) {
            try {
                // try opening browser
                context.startActivity(Intent(ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            } catch (e1: ActivityNotFoundException) {
                // give up and cry
                Toast.makeText(context, context.getString(R.string.error_unable_to_rate_app), Toast.LENGTH_LONG).show()
                logWarning("AboutActivity", "App rating without google play store support", e1)
            }
        }
    }

    fun openEmail(context: Context) = context.startActivity(
        Intent(Intent.ACTION_SENDTO)
            .setData(Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.email_address)))
    )

    fun openDiscord(context: Context) = context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.discord_url))))

    fun openGitHub(context: Context) = context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.github_url))))

    fun openWebsite(context: Context) = context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.website_url))))

    /**
     * Opens the settings page.
     */
    fun Settings() = startActivity(SettingsActivity::class.java)

    /**
     * Opens the welcome tutorial.
     */
    fun Tutorial() = startActivity(SetupActivity::class.java)

    /**
     * Opens the about page.
     */
    fun About() = startActivity(AboutActivity::class.java)

    /**
     * Opens the help page.
     */
    fun Help() = startActivity(HelpActivity::class.java)

    /**
     * Opens the faq page.
     */
    fun FAQ() = startActivity(FAQActivity::class.java)

    /**
     * Opens the contribution popup.
     */
    fun Contribute() = startActivity(ContributorActivity::class.java)

    /**
     * Opens the contribution popup without option to enroll.
     */
    fun ContributeNoEnroll() = startActivity(
        Intent(baseActivity.get(), ContributorActivity::class.java)
            .putExtra(ContributorActivity.INTENT_HIDE_ENROLLMENT, true)
    )

    /**
     * Opens the update installation page.
     */
    fun UpdateInstallation(isDownloaded: Boolean, updateData: UpdateData?) = startActivity(
        Intent(baseActivity.get(), InstallActivity::class.java)
            .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, !isDownloaded)
            .putExtra(InstallActivity.INTENT_UPDATE_DATA, updateData)
    )

    private fun <T> startActivity(activityClass: Class<T>) = startActivity(Intent(baseActivity.get(), activityClass))

    private fun startActivity(intent: Intent) = baseActivity.get()!!.apply {
        startActivity(intent.addFlags(FLAG_ACTIVITY_NEW_TASK), makeSceneTransitionAnimationBundle())
    }

    fun dispose() = baseActivity.clear()
}

fun Activity.makeSceneTransitionAnimationBundle() = ActivityOptionsCompat.makeSceneTransitionAnimation(this, *createTransitionPairs().toTypedArray()).toBundle()

/**
 * Creates transition pairs for status bar, navigation bar, and app bar
 */
fun Activity.createTransitionPairs(): List<Pair<View, String>> = arrayListOf<Pair<View, String>>().apply {
    val appBarLayout = findViewById<AppBarLayout>(R.id.appBar)
    val statusBar = findViewById<View>(android.R.id.statusBarBackground)
    val navigationBar = findViewById<View>(android.R.id.navigationBarBackground)

    if (statusBar != null) {
        add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
    }

    if (navigationBar != null) {
        add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
    }

    if (appBarLayout != null) {
        add(Pair.create(appBarLayout, "toolbar"))
    }
}
