package com.oxygenupdater.activities

import android.app.Activity
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.oxygenupdater.compose.activities.MainActivity
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.extensions.startOnboardingActivity
import com.oxygenupdater.internal.settings.PrefManager

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) = installSplashScreen().setKeepOnScreenCondition {
        true // keep the splash screen visible for this Activity
    }.also {
        super.onCreate(savedInstanceState)
        chooseActivityToLaunch()
        finish()
    }

    private fun chooseActivityToLaunch() {
        // Since app shortcuts open this activity, we need to forward the
        // corresponding `startPage` so that MainActivity eventually receives it
        val startPage = when (intent?.action) {
            ACTION_PAGE_UPDATE -> MainActivity.PAGE_UPDATE
            ACTION_PAGE_NEWS -> MainActivity.PAGE_NEWS
            ACTION_PAGE_DEVICE -> MainActivity.PAGE_DEVICE
            else -> MainActivity.PAGE_UPDATE
        }

        if (!PrefManager.getBoolean(PrefManager.PROPERTY_SETUP_DONE, false)) {
            startOnboardingActivity(startPage) // setup needed; launch Onboarding
        } else startMainActivity(startPage) // setup complete; launch Main
    }

    companion object {
        private const val ACTION_PAGE_UPDATE = "com.oxygenupdater.action.page_update"
        private const val ACTION_PAGE_NEWS = "com.oxygenupdater.action.page_news"
        private const val ACTION_PAGE_DEVICE = "com.oxygenupdater.action.page_device"
    }
}
