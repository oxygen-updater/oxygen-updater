package com.arjanvlek.oxygenupdater.activities

import android.os.Bundle
import android.view.MenuItem
import com.arjanvlek.oxygenupdater.R

class HelpActivity : SupportActionBarActivity() {

    public override fun onCreate(
        savedInstanceSate: Bundle?
    ) = super.onCreate(savedInstanceSate).also {
        setContentView(R.layout.activity_help)
    }

    override fun onBackPressed() = finish()

    /**
     * Respond to the action bar's Up/Home button.
     * Delegate to [onBackPressed] if [android.R.id.home] is clicked, otherwise call `super`
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }
}
