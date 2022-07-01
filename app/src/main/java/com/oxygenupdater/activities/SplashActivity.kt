package com.oxygenupdater.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oxygenupdater.R
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.extensions.startOnboardingActivity
import com.oxygenupdater.internal.settings.PrefManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        // API 21 & 22 don't draw SVGs properly, so we override the theme on these APIs to draw a normal background,
        // and use setContentView(R.layout.splash_activity) only on those API levels.
        // On API 23 and above, we use the recommended way to display splash screens - using a static windowBackground
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            setContentView(R.layout.activity_splash)
        }

        chooseActivityToLaunch()
    }

    private fun chooseActivityToLaunch() {
        // Mark the welcome tutorial as finished if the user is moving from older app version.
        // This is checked by either having stored update information for offline viewing,
        // or if the last update checked date is set (if user always had up to date system and never viewed update information before)
        if (!PrefManager.getBoolean(PrefManager.PROPERTY_SETUP_DONE, false)
            && (PrefManager.checkIfOfflineUpdateDataIsAvailable() || PrefManager.contains(PrefManager.PROPERTY_UPDATE_CHECKED_DATE))
        ) {
            PrefManager.putBoolean(PrefManager.PROPERTY_SETUP_DONE, true)
        }

        // Since app shortcuts open this activity, we need to forward the
        // corresponding `startPage` so that MainActivity eventually receives it
        val startPage = when (intent?.action) {
            ACTION_PAGE_UPDATE -> MainActivity.PAGE_UPDATE
            ACTION_PAGE_NEWS -> MainActivity.PAGE_NEWS
            ACTION_PAGE_DEVICE -> MainActivity.PAGE_DEVICE
            else -> MainActivity.PAGE_UPDATE
        }

        if (!PrefManager.getBoolean(PrefManager.PROPERTY_SETUP_DONE, false)) {
            // Launch OnboardingActivity since the app hasn't been setup yet
            startOnboardingActivity(startPage)
        } else {
            // Setup is complete, launch MainActivity
            startMainActivity(startPage)
        }

        finish()
    }

    companion object {
        private const val ACTION_PAGE_UPDATE = "com.oxygenupdater.action.page_update"
        private const val ACTION_PAGE_NEWS = "com.oxygenupdater.action.page_news"
        private const val ACTION_PAGE_DEVICE = "com.oxygenupdater.action.page_device"
    }
}
