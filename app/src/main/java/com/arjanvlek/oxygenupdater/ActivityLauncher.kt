package com.arjanvlek.oxygenupdater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.widget.Toast
import com.arjanvlek.oxygenupdater.activities.AboutActivity
import com.arjanvlek.oxygenupdater.activities.ContributorActivity
import com.arjanvlek.oxygenupdater.activities.FAQActivity
import com.arjanvlek.oxygenupdater.activities.HelpActivity
import com.arjanvlek.oxygenupdater.activities.InstallActivity
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.activities.SettingsActivity
import com.arjanvlek.oxygenupdater.extensions.makeSceneTransitionAnimationBundle
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
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
     * Opens the main page.
     */
    fun Main() = startActivity(MainActivity::class.java, false)

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
     * Opens the update installation page.
     */
    fun UpdateInstallation(isDownloaded: Boolean, updateData: UpdateData?) = startActivity(
        Intent(baseActivity.get(), InstallActivity::class.java)
            .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, !isDownloaded)
            .putExtra(InstallActivity.INTENT_UPDATE_DATA, updateData)
    )

    private fun <T> startActivity(activityClass: Class<T>, shouldMakeSceneTransitionBundle: Boolean = true) =
        startActivity(Intent(baseActivity.get(), activityClass), shouldMakeSceneTransitionBundle)

    private fun startActivity(intent: Intent, shouldMakeSceneTransitionBundle: Boolean = true) = baseActivity.get()!!.apply {
        startActivity(
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK),
            if (shouldMakeSceneTransitionBundle) makeSceneTransitionAnimationBundle() else null
        )
    }

    fun dispose() = baseActivity.clear()
}
