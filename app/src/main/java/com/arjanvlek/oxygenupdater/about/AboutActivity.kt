package com.arjanvlek.oxygenupdater.about

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : SupportActionBarActivity() {
    public override fun onCreate(savedInstanceSate: Bundle?) {
        super.onCreate(savedInstanceSate)
        setContentView(R.layout.activity_about)

        val activityLauncher = ActivityLauncher(this)

        // Set the version number of the app in the version number field.
        aboutVersionNumberView.text = String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME)
        // Make the links in the background story clickable.
        aboutBackgroundStoryView.movementMethod = LinkMovementMethod.getInstance()
        // Set onClick listener to Google Play rate button.
        aboutRateButton.setOnClickListener { activityLauncher.launchPlayStorePage(this) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
