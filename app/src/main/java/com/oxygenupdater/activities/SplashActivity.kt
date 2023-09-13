package com.oxygenupdater.activities

import android.app.Activity
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
            ActionPageUpdate -> MainActivity.PageUpdate
            ActionPageNews -> MainActivity.PageNews
            ActionPageDevice -> MainActivity.PageDevice
            else -> MainActivity.PageUpdate
        }

        if (!PrefManager.getBoolean(PrefManager.KeySetupDone, false)) {
            startOnboardingActivity(startPage) // setup needed; launch Onboarding
        } else startMainActivity(startPage) // setup complete; launch Main
    }

    companion object {
        private const val ActionPageUpdate = "com.oxygenupdater.action.page_update"
        private const val ActionPageNews = "com.oxygenupdater.action.page_news"
        private const val ActionPageDevice = "com.oxygenupdater.action.page_device"
    }
}
