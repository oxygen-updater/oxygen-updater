package com.arjanvlek.oxygenupdater.help

import android.os.Bundle
import android.view.MenuItem
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity

class HelpActivity : SupportActionBarActivity() {

    public override fun onCreate(savedInstanceSate: Bundle?) {
        super.onCreate(savedInstanceSate)
        setContentView(R.layout.activity_help)
    }

    override fun onBackPressed() = finish()

    /**
     * Respond to the action bar's Up/Home button
     */
    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        finish()
        true
    } else {
        super.onOptionsItemSelected(item)
    }
}
