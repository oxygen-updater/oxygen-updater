package com.arjanvlek.oxygenupdater.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.fragments.SettingsFragment
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.SetupUtils
import org.koin.android.ext.android.inject

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class SettingsActivity : SupportActionBarActivity() {

    private val settingsManager by inject<SettingsManager>()

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
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() = if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
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
