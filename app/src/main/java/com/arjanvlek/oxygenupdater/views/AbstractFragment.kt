package com.arjanvlek.oxygenupdater.views

import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.settings.SettingsManager


abstract class AbstractFragment : Fragment() {
    private var applicationData: ApplicationData? = null
    private var settingsManager: SettingsManager? = null

    val serverConnector: ServerConnector
        get() {
            if (applicationData == null && activity != null) {
                applicationData = activity!!.application as ApplicationData
            }
            return if (applicationData != null) applicationData!!.getServerConnector() else ServerConnector(SettingsManager(null))
        }

    fun getApplicationData(): ApplicationData {
        if (applicationData == null) {
            applicationData = try {
                activity!!.application as ApplicationData
            } catch (e: Exception) {
                logError("AbstractFragment", "FAILED to get Application instance", e)
                // Return empty application data which can still be used for SystemVersionProperties and to check for root access.
                ApplicationData()
            }

        }
        return applicationData as ApplicationData
    }

    fun getSettingsManager(): SettingsManager {
        if (applicationData == null && activity != null) {
            applicationData = activity!!.application as ApplicationData
        }

        if (settingsManager == null) {
            settingsManager = SettingsManager(applicationData)
        }

        return settingsManager as SettingsManager
    }

    companion object {
        //Test devices for ads.
        val ADS_TEST_DEVICES = listOf("BE7E0AF85E0332807B1EA3FE4236F93C", "0FD2DE005EB9DD19BD02FB2CD4D87902")
    }
}
