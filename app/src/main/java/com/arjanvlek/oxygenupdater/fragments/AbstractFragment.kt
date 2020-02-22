package com.arjanvlek.oxygenupdater.fragments

import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.utils.Logger.logError

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

    var settingsManager: SettingsManager? = null
        get() {
            if (application == null && activity != null) {
                application = activity!!.application as OxygenUpdater
            }

            if (field == null) {
                field = SettingsManager(application)
            }

            return field
        }
        private set

    val serverConnector: ServerConnector?
        get() {
            if (application == null && activity != null) {
                application = activity!!.application as OxygenUpdater
            }

            return application?.serverConnector ?: ServerConnector(SettingsManager(null))
        }
}
