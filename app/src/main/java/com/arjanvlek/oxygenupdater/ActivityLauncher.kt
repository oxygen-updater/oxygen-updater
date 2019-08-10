package com.arjanvlek.oxygenupdater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.arjanvlek.oxygenupdater.about.AboutActivity
import com.arjanvlek.oxygenupdater.contribution.ContributorActivity
import com.arjanvlek.oxygenupdater.contribution.ContributorActivity.Companion.INTENT_HIDE_ENROLLMENT
import com.arjanvlek.oxygenupdater.faq.FAQActivity
import com.arjanvlek.oxygenupdater.help.HelpActivity
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.installation.InstallActivity.Companion.INTENT_SHOW_DOWNLOAD_PAGE
import com.arjanvlek.oxygenupdater.installation.InstallActivity.Companion.INTENT_UPDATE_DATA
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.settings.SettingsActivity
import com.arjanvlek.oxygenupdater.setupwizard.SetupActivity
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData
import java.lang.ref.WeakReference

class ActivityLauncher(baseActivity: Activity) {

    private val baseActivity: WeakReference<Activity> = WeakReference(baseActivity)

    fun launchPlayStorePage(context: Context) {
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
                Toast.makeText(context, context.getString(R.string.error_unable_to_rate_app), LENGTH_LONG).show()
                logWarning("AboutActivity", "App rating without google play store support", e1)
            }
        }
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
     * Opens the contribution popup.
     */
    fun Contribute() {
        startActivity(ContributorActivity::class.java)
    }

    /**
     * Opens the contribution popup without option to enroll.
     */
    fun Contribute_noenroll() {
        val i = Intent(baseActivity.get(), ContributorActivity::class.java)
        i.putExtra(INTENT_HIDE_ENROLLMENT, true)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        baseActivity.get()?.startActivity(i)
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
     * Opens the update installation page.
     */
    fun UpdateInstallation(isDownloaded: Boolean, updateData: UpdateData) {
        val i = Intent(baseActivity.get(), InstallActivity::class.java)
        i.putExtra(INTENT_SHOW_DOWNLOAD_PAGE, !isDownloaded)
        i.putExtra(INTENT_UPDATE_DATA, updateData)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        baseActivity.get()?.startActivity(i)
    }

    private fun <T> startActivity(activityClass: Class<T>) {
        val i = Intent(baseActivity.get(), activityClass)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        baseActivity.get()?.startActivity(i)
    }

    fun dispose() {
        baseActivity.clear()
    }


}
