package com.oxygenupdater.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import com.oxygenupdater.R
import com.oxygenupdater.fragments.SettingsFragment
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.SetupUtils

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class SettingsActivity : SupportActionBarActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        setContentView(R.layout.activity_settings)

        supportFragmentManager.commitNow {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(R.id.settings_container, SettingsFragment(), "Settings")
        }
    }

    private fun showSettingsWarning() {
        val deviceId = SettingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = SettingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() = if (SettingsManager.checkIfSetupScreenHasBeenCompleted()) {
        NavUtils.navigateUpFromSameTask(this)
    } else {
        showSettingsWarning()
    }

    /**
     * Respond to the action bar's Up/Home button.
     * Delegate to [onBackPressed] if [android.R.id.home] is clicked, otherwise call `super`
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
