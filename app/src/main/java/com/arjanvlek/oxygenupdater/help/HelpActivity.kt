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

    override fun onBackPressed() {
        finish()
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
