package com.arjanvlek.oxygenupdater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.widget.Toast
import com.arjanvlek.oxygenupdater.about.AboutActivity
import com.arjanvlek.oxygenupdater.contribution.ContributorActivity
import com.arjanvlek.oxygenupdater.faq.FAQActivity
import com.arjanvlek.oxygenupdater.help.HelpActivity
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.settings.SettingsActivity
import com.arjanvlek.oxygenupdater.setupwizard.SetupActivity
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

    fun openEmail(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
            .setData(Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.email_address)))

        context.startActivity(intent)
    }

    fun openDiscord(context: Context) {
        context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.discord_url))))
    }

    fun openGitHub(context: Context) {
        context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.github_url))))
    }

    fun openWebsite(context: Context) {
        context.startActivity(Intent(ACTION_VIEW, Uri.parse(context.getString(R.string.website_url))))
    }

    /**
     * Opens the settings page.
     */
    fun Settings() {
        startActivity(SettingsActivity::class.java)
    }

    /**
     * Opens the welcome tutorial.
     */
    fun Tutorial() {
        startActivity(SetupActivity::class.java)
    }

    /**
     * Opens the about page.
     */
    fun About() {
        startActivity(AboutActivity::class.java)
    }

    /**
     * Opens the help page.
     */
    fun Help() {
        startActivity(HelpActivity::class.java)
    }

    /**
     * Opens the faq page.
     */
    fun FAQ() {
        startActivity(FAQActivity::class.java)
    }

    /**
     * Opens the contribution popup.
     */
    fun Contribute() {
        startActivity(ContributorActivity::class.java)
    }

    /**
     * Opens the contribution popup without option to enroll.
     */
    fun ContributeNoEnroll() {
        val intent = Intent(baseActivity.get(), ContributorActivity::class.java)
            .putExtra(ContributorActivity.INTENT_HIDE_ENROLLMENT, true)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        baseActivity.get()!!.startActivity(intent)
    }

    /**
     * Opens the update installation page.
     */
    fun UpdateInstallation(isDownloaded: Boolean, updateData: UpdateData?) {
        val intent = Intent(baseActivity.get(), InstallActivity::class.java)
            .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, !isDownloaded)
            .putExtra(InstallActivity.INTENT_UPDATE_DATA, updateData)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        baseActivity.get()!!.startActivity(intent)
    }

    private fun <T> startActivity(activityClass: Class<T>) {
        val intent = Intent(baseActivity.get(), activityClass)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        baseActivity.get()!!.startActivity(intent)
    }

    fun dispose() {
        baseActivity.clear()
    }
}
