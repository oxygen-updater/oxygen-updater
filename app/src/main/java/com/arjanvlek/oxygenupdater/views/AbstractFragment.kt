package com.arjanvlek.oxygenupdater.views

import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.settings.SettingsManager

abstract class AbstractFragment : Fragment() {
    var applicationData: ApplicationData? = null
        get() {
            if (field == null) {
                field = try {
                    activity!!.application as ApplicationData
                } catch (e: Exception) {
                    logError("AbstractFragment", "FAILED to get Application instance", e)

                    // Return empty application data which can still be used for SystemVersionProperties and to check for root access.
                    ApplicationData()
                }
            }

            return field
        }
        private set

    var settingsManager: SettingsManager? = null
        get() {
            if (applicationData == null && activity != null) {
                applicationData = activity!!.application as ApplicationData
            }

            if (field == null) {
                field = SettingsManager(applicationData)
            }

            return field
        }
        private set

    val serverConnector: ServerConnector?
        get() {
            if (applicationData == null && activity != null) {
                applicationData = activity!!.application as ApplicationData
            }

            return applicationData?.serverConnector ?: ServerConnector(SettingsManager(null))
        }
}
