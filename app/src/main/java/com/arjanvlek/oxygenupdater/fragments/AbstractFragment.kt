package com.arjanvlek.oxygenupdater.fragments

import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import org.koin.android.ext.android.inject

abstract class AbstractFragment : Fragment() {

    var application: OxygenUpdater? = null
        get() {
            if (field == null) {
                field = try {
                    activity!!.application as OxygenUpdater
                } catch (e: Exception) {
                    logError("AbstractFragment", "FAILED to get Application instance", e)

                    // Return empty application data which can still be used for SystemVersionProperties and to check for root access.
                    OxygenUpdater()
                }
            }

            return field
        }
        private set

    val settingsManager by inject<SettingsManager>()
}
